package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.AuctionActionRequest;
import org.dcoffice.cachar.dto.AuctionBidRequest;
import org.dcoffice.cachar.dto.AuctionListingRequest;
import org.dcoffice.cachar.entity.AuctionAuditTrail;
import org.dcoffice.cachar.entity.AuctionBid;
import org.dcoffice.cachar.entity.AuctionDocument;
import org.dcoffice.cachar.entity.AuctionListing;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.repository.AuctionAuditTrailRepository;
import org.dcoffice.cachar.repository.AuctionBidRepository;
import org.dcoffice.cachar.repository.AuctionListingRepository;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.OfficerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuctionService {
    private final AuctionListingRepository listingRepository;
    private final AuctionBidRepository bidRepository;
    private final AuctionAuditTrailRepository auditRepository;
    private final CitizenRepository citizenRepository;
    private final OfficerRepository officerRepository;
    private final CounterService counterService;

    public AuctionService(
            AuctionListingRepository listingRepository,
            AuctionBidRepository bidRepository,
            AuctionAuditTrailRepository auditRepository,
            CitizenRepository citizenRepository,
            OfficerRepository officerRepository,
            CounterService counterService
    ) {
        this.listingRepository = listingRepository;
        this.bidRepository = bidRepository;
        this.auditRepository = auditRepository;
        this.citizenRepository = citizenRepository;
        this.officerRepository = officerRepository;
        this.counterService = counterService;
    }

    public List<AuctionListing> publicListings(String status, String category, Integer wardNumber) {
        ensureSeedAuctions();
        refreshTimedStatuses();
        return listingRepository.findAll().stream()
                .filter(listing -> List.of("PUBLISHED", "LIVE", "CLOSED", "AWARDED").contains(listing.getStatus()))
                .filter(listing -> status == null || status.isBlank() || listing.getStatus().equalsIgnoreCase(status))
                .filter(listing -> category == null || category.isBlank() || safe(listing.getCategory()).equalsIgnoreCase(category))
                .filter(listing -> wardNumber == null || wardNumber.equals(listing.getWardNumber()))
                .sorted(Comparator.comparing(AuctionListing::getEndAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public AuctionListing publicDetail(String auctionId) {
        ensureSeedAuctions();
        refreshTimedStatuses();
        AuctionListing listing = findListing(auctionId);
        if (!List.of("PUBLISHED", "LIVE", "CLOSED", "AWARDED").contains(listing.getStatus())) {
            throw new IllegalArgumentException("Auction is not available for public view");
        }
        return listing;
    }

    public Map<String, Object> bidSummary(String auctionId) {
        AuctionListing listing = publicDetail(auctionId);
        Map<String, Object> response = new HashMap<>();
        response.put("auctionId", listing.getAuctionId());
        response.put("highestBid", safeAmount(listing.getCurrentHighestBid()));
        response.put("minimumNextBid", minimumNextBid(listing));
        response.put("totalBids", bidRepository.countByAuctionId(listing.getAuctionId()));
        response.put("timeRemainingSeconds", remainingSeconds(listing));
        return response;
    }

    public AuctionBid placeBid(String auctionId, String citizenId, AuctionBidRequest request) {
        refreshTimedStatuses();
        AuctionListing listing = findListing(auctionId);
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found"));
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new IllegalArgumentException("Terms must be accepted before placing a bid");
        }
        if (!"LIVE".equals(listing.getStatus())) {
            throw new IllegalArgumentException("Bidding is allowed only for live auctions");
        }
        LocalDateTime now = LocalDateTime.now();
        if (listing.getStartAt() == null || listing.getEndAt() == null || now.isBefore(listing.getStartAt()) || now.isAfter(listing.getEndAt())) {
            throw new IllegalArgumentException("Auction is outside bidding window");
        }
        BigDecimal bidAmount = safeAmount(request.getBidAmount());
        BigDecimal minBid = minimumNextBid(listing);
        if (bidAmount.compareTo(minBid) < 0) {
            throw new IllegalArgumentException("Bid must be at least " + minBid);
        }

        if (listing.getCurrentHighestBidId() != null) {
            bidRepository.findByBidId(listing.getCurrentHighestBidId()).ifPresent(previous -> {
                previous.setBidStatus("OUTBID");
                bidRepository.save(previous);
            });
        }

        AuctionBid bid = new AuctionBid();
        bid.setBidId("SMC-BID-" + String.format("%06d", counterService.getNextSequence("auctionBid")));
        bid.setAuctionId(listing.getAuctionId());
        bid.setBidderCitizenId(citizenId);
        bid.setSmcCitizenId(citizen.getSmcCitizenId());
        bid.setBidderName(citizen.getName());
        bid.setBidderMobile(citizen.getMobileNumber());
        bid.setBidderEmail(citizen.getEmail());
        bid.setBidderType(blankToDefault(request.getBidderType(), "CITIZEN"));
        bid.setBusinessName(request.getBusinessName());
        bid.setGstNumber(request.getGstNumber());
        bid.setTradeLicenseNumber(request.getTradeLicenseNumber());
        bid.setBidAmount(bidAmount);
        bid.setBidStatus("WINNING");
        bid.setTermsAccepted(true);
        bid.setEmdPaymentStatus(safeAmount(listing.getEmdAmount()).compareTo(BigDecimal.ZERO) > 0 ? "PENDING" : "NOT_REQUIRED");
        bid.setSubmittedAt(now);
        bid = bidRepository.save(bid);

        listing.setCurrentHighestBid(bidAmount);
        listing.setCurrentHighestBidId(bid.getBidId());
        listing.setUpdatedAt(now);
        listingRepository.save(listing);
        audit(listing.getAuctionId(), "BID_PLACED", listing.getStatus(), listing.getStatus(), citizenId, citizen.getName(), "CITIZEN", "Bid " + bid.getBidId() + " placed");
        return bid;
    }

    public List<AuctionBid> myBids(String citizenId) {
        return bidRepository.findByBidderCitizenIdOrderBySubmittedAtDesc(citizenId);
    }

    public AuctionListing createAuction(String officerId, AuctionListingRequest request) {
        Officer officer = officer(officerId);
        AuctionListing listing = new AuctionListing();
        listing.setAuctionId("SMC-AUC-" + String.format("%06d", counterService.getNextSequence("auctionListing")));
        applyRequest(listing, request);
        listing.setStatus("DRAFT");
        listing.setCreatedByOfficerId(officer.getId());
        listing.setCreatedByOfficerName(officer.getName());
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);
        audit(listing.getAuctionId(), "AUCTION_CREATED", null, "DRAFT", officer.getId(), officer.getName(), role(officer), "Auction created");
        return listing;
    }

    public AuctionListing updateAuction(String auctionId, String officerId, AuctionListingRequest request) {
        Officer officer = officer(officerId);
        AuctionListing listing = findListing(auctionId);
        if (List.of("CANCELLED", "AWARDED").contains(listing.getStatus())) {
            throw new IllegalArgumentException("Cancelled or awarded auctions cannot be edited");
        }
        String oldStatus = listing.getStatus();
        applyRequest(listing, request);
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);
        audit(listing.getAuctionId(), "AUCTION_UPDATED", oldStatus, listing.getStatus(), officer.getId(), officer.getName(), role(officer), "Auction details updated");
        return listing;
    }

    public AuctionListing publish(String auctionId, String officerId) {
        Officer officer = officer(officerId);
        AuctionListing listing = findListing(auctionId);
        validatePublishable(listing);
        String oldStatus = listing.getStatus();
        listing.setStatus(LocalDateTime.now().isBefore(listing.getStartAt()) ? "PUBLISHED" : "LIVE");
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);
        audit(listing.getAuctionId(), "AUCTION_PUBLISHED", oldStatus, listing.getStatus(), officer.getId(), officer.getName(), role(officer), "Auction published");
        return listing;
    }

    public AuctionListing cancel(String auctionId, String officerId, AuctionActionRequest request) {
        Officer officer = officer(officerId);
        AuctionListing listing = findListing(auctionId);
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        String oldStatus = listing.getStatus();
        listing.setStatus("CANCELLED");
        listing.setCancellationReason(request.getReason());
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);
        audit(listing.getAuctionId(), "AUCTION_CANCELLED", oldStatus, "CANCELLED", officer.getId(), officer.getName(), role(officer), request.getReason());
        return listing;
    }

    public AuctionListing award(String auctionId, String officerId, AuctionActionRequest request) {
        Officer officer = officer(officerId);
        refreshTimedStatuses();
        AuctionListing listing = findListing(auctionId);
        if (listing.getEndAt() != null && LocalDateTime.now().isBefore(listing.getEndAt())) {
            throw new IllegalArgumentException("Auction cannot be awarded before end time");
        }
        List<AuctionBid> bids = bidRepository.findByAuctionIdOrderByBidAmountDesc(listing.getAuctionId());
        if (bids.isEmpty()) {
            throw new IllegalArgumentException("Cannot award auction without bids");
        }
        AuctionBid winningBid = request.getBidId() == null || request.getBidId().isBlank()
                ? bids.get(0)
                : bidRepository.findByBidId(request.getBidId()).orElseThrow(() -> new IllegalArgumentException("Bid not found"));
        String oldStatus = listing.getStatus();
        winningBid.setBidStatus("AWARDED");
        bidRepository.save(winningBid);
        listing.setStatus("AWARDED");
        listing.setAwardedBidId(winningBid.getBidId());
        listing.setAwardedBidderId(winningBid.getBidderCitizenId());
        listing.setAwardedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);
        audit(listing.getAuctionId(), "AUCTION_AWARDED", oldStatus, "AWARDED", officer.getId(), officer.getName(), role(officer), blankToDefault(request.getRemarks(), "Awarded to highest eligible bidder"));
        return listing;
    }

    public List<AuctionListing> officerAuctions() {
        ensureSeedAuctions();
        refreshTimedStatuses();
        return listingRepository.findAll().stream()
                .sorted(Comparator.comparing(AuctionListing::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Map<String, Object> officerAuctionDetail(String auctionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("auction", findListing(auctionId));
        response.put("bids", bidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId));
        response.put("auditTrail", auditRepository.findByAuctionIdOrderByTimestampDesc(auctionId));
        return response;
    }

    public List<AuctionBid> officerBids(String auctionId) {
        return bidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId);
    }

    public List<AuctionAuditTrail> auditTrail(String auctionId) {
        return auditRepository.findByAuctionIdOrderByTimestampDesc(auctionId);
    }

    public Map<String, Object> dashboard() {
        ensureSeedAuctions();
        refreshTimedStatuses();
        List<AuctionListing> listings = listingRepository.findAll();
        List<AuctionBid> bids = bidRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("totalAuctions", listings.size());
        response.put("draftAuctions", count(listings, "DRAFT"));
        response.put("publishedAuctions", count(listings, "PUBLISHED"));
        response.put("liveAuctions", count(listings, "LIVE"));
        response.put("closedAuctions", count(listings, "CLOSED"));
        response.put("awardedAuctions", count(listings, "AWARDED"));
        response.put("cancelledAuctions", count(listings, "CANCELLED"));
        response.put("totalBidValue", bids.stream().map(AuctionBid::getBidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("highestValueAuction", listings.stream().max(Comparator.comparing(AuctionListing::getCurrentHighestBid, Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null));
        response.put("auctionsClosingToday", listings.stream().filter(this::closingToday).collect(Collectors.toList()));
        response.put("auctionsWithoutBids", listings.stream().filter(a -> bidRepository.countByAuctionId(a.getAuctionId()) == 0).collect(Collectors.toList()));
        response.put("categoryWiseAuctions", groupCount(listings, AuctionListing::getCategory));
        response.put("wardWiseAuctions", groupCount(listings, listing -> listing.getWardNumber() == null ? "Unmapped" : "Ward " + listing.getWardNumber()));
        response.put("recentBids", bids.stream().sorted(Comparator.comparing(AuctionBid::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(10).collect(Collectors.toList()));
        response.put("pendingAwardAuctions", listings.stream().filter(a -> "CLOSED".equals(a.getStatus()) && bidRepository.countByAuctionId(a.getAuctionId()) > 0).collect(Collectors.toList()));
        return response;
    }

    private void applyRequest(AuctionListing listing, AuctionListingRequest request) {
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setCategory(request.getCategory());
        listing.setResourceType(request.getResourceType());
        listing.setDepartment(request.getDepartment());
        listing.setWardNumber(request.getWardNumber());
        listing.setWardName(request.getWardName());
        listing.setZone(request.getZone());
        listing.setLocality(request.getLocality());
        listing.setAddress(request.getAddress());
        listing.setLatitude(request.getLatitude());
        listing.setLongitude(request.getLongitude());
        listing.setBasePrice(safeAmount(request.getBasePrice()));
        listing.setReservePrice(safeAmount(request.getReservePrice()));
        listing.setBidIncrement(safeAmount(request.getBidIncrement()).compareTo(BigDecimal.ZERO) > 0 ? request.getBidIncrement() : BigDecimal.valueOf(1000));
        listing.setEmdAmount(safeAmount(request.getEmdAmount()));
        listing.setStartAt(request.getStartAt());
        listing.setEndAt(request.getEndAt());
        listing.setInspectionAt(request.getInspectionAt());
        listing.setEligibilityCriteria(request.getEligibilityCriteria());
        listing.setTermsAndConditions(request.getTermsAndConditions());
    }

    private void validatePublishable(AuctionListing listing) {
        if (listing.getTitle() == null || listing.getTitle().isBlank()) throw new IllegalArgumentException("Title is required");
        if (listing.getBasePrice() == null || listing.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Base price must be greater than zero");
        if (listing.getBidIncrement() == null || listing.getBidIncrement().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Bid increment must be greater than zero");
        if (listing.getStartAt() == null || listing.getEndAt() == null || !listing.getEndAt().isAfter(listing.getStartAt())) throw new IllegalArgumentException("Valid start and end time are required");
    }

    private AuctionListing findListing(String auctionId) {
        return listingRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + auctionId));
    }

    private Officer officer(String officerId) {
        return officerRepository.findById(officerId)
                .orElseGet(() -> {
                    Officer fallback = new Officer();
                    fallback.setId(officerId);
                    fallback.setName("SMC Officer");
                    return fallback;
                });
    }

    private void audit(String auctionId, String action, String oldStatus, String newStatus, String actorId, String actorName, String actorRole, String remarks) {
        AuctionAuditTrail audit = new AuctionAuditTrail();
        audit.setAuctionId(auctionId);
        audit.setAction(action);
        audit.setPreviousStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setActorId(actorId);
        audit.setActorName(actorName);
        audit.setActorRole(actorRole);
        audit.setRemarks(remarks);
        audit.setTimestamp(LocalDateTime.now());
        auditRepository.save(audit);
    }

    private void refreshTimedStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (AuctionListing listing : listingRepository.findAll()) {
            String oldStatus = listing.getStatus();
            if ("PUBLISHED".equals(oldStatus) && listing.getStartAt() != null && !now.isBefore(listing.getStartAt()) && listing.getEndAt() != null && now.isBefore(listing.getEndAt())) {
                listing.setStatus("LIVE");
            } else if ("LIVE".equals(oldStatus) && listing.getEndAt() != null && !now.isBefore(listing.getEndAt())) {
                listing.setStatus("CLOSED");
            }
            if (!oldStatus.equals(listing.getStatus())) {
                listing.setUpdatedAt(now);
                listingRepository.save(listing);
                audit(listing.getAuctionId(), "STATUS_AUTO_UPDATED", oldStatus, listing.getStatus(), "SYSTEM", "System", "SYSTEM", "Auction status updated by schedule");
            }
        }
    }

    private BigDecimal minimumNextBid(AuctionListing listing) {
        BigDecimal highest = safeAmount(listing.getCurrentHighestBid());
        return highest.compareTo(BigDecimal.ZERO) > 0
                ? highest.add(safeAmount(listing.getBidIncrement()))
                : safeAmount(listing.getBasePrice());
    }

    private long remainingSeconds(AuctionListing listing) {
        if (listing.getEndAt() == null) return 0;
        return Math.max(0, Duration.between(LocalDateTime.now(), listing.getEndAt()).getSeconds());
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String role(Officer officer) {
        return officer.getRole() == null ? "OFFICER" : officer.getRole().name();
    }

    private long count(List<AuctionListing> listings, String status) {
        return listings.stream().filter(listing -> status.equals(listing.getStatus())).count();
    }

    private boolean closingToday(AuctionListing listing) {
        return listing.getEndAt() != null && listing.getEndAt().toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    private Map<String, Long> groupCount(List<AuctionListing> listings, java.util.function.Function<AuctionListing, String> key) {
        return listings.stream().collect(Collectors.groupingBy(item -> blankToDefault(key.apply(item), "Unmapped"), Collectors.counting()));
    }

    private void ensureSeedAuctions() {
        if (listingRepository.count() > 0) return;
        List<AuctionListing> seeds = new ArrayList<>();
        seeds.add(seed("Parking lot lease near Rangirkhari", "Lease rights for managed parking operations near Rangirkhari market area.", "PARKING", "Parking Lot Lease", 12, "Ward 12 - Rangirkhari", "Rangirkhari", 75000, 5000, "LIVE"));
        seeds.add(seed("Market stall allotment in Itkhola", "Annual allotment auction for municipal market stall at Itkhola.", "MARKET_STALL", "Market Stall", 8, "Ward 8 - Itkhola", "Itkhola", 25000, 1000, "PUBLISHED"));
        seeds.add(seed("Advertisement hoarding rights in Tarapur", "Advertisement rights for approved municipal hoarding location in Tarapur.", "ADVERTISEMENT", "Hoarding Rights", 15, "Ward 15 - Tarapur", "Tarapur", 120000, 10000, "LIVE"));
        seeds.add(seed("Scrap material disposal from SMC yard", "Auction for disposal of segregated municipal scrap from SMC yard.", "SCRAP", "Scrap Disposal", 4, "Ward 4", "SMC Yard", 18000, 1000, "PUBLISHED"));
        for (AuctionListing seed : seeds) {
            listingRepository.save(seed);
            audit(seed.getAuctionId(), "AUCTION_CREATED", null, seed.getStatus(), "SYSTEM", "System", "SYSTEM", "Seed auction created");
        }
    }

    private AuctionListing seed(String title, String description, String category, String resourceType, int wardNumber, String wardName, String locality, int basePrice, int increment, String status) {
        AuctionListing listing = new AuctionListing();
        listing.setAuctionId("SMC-AUC-" + String.format("%06d", counterService.getNextSequence("auctionListing")));
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setCategory(category);
        listing.setResourceType(resourceType);
        listing.setDepartment("Revenue Department");
        listing.setWardNumber(wardNumber);
        listing.setWardName(wardName);
        listing.setZone(wardNumber <= 8 ? "Central Zone" : "South Zone");
        listing.setLocality(locality);
        listing.setAddress(locality + ", Silchar, Cachar");
        listing.setBasePrice(BigDecimal.valueOf(basePrice));
        listing.setReservePrice(BigDecimal.valueOf(basePrice));
        listing.setBidIncrement(BigDecimal.valueOf(increment));
        listing.setEmdAmount(BigDecimal.valueOf(Math.max(1000, basePrice / 10)));
        listing.setStartAt("LIVE".equals(status) ? LocalDateTime.now().minusHours(2) : LocalDateTime.now().plusDays(1));
        listing.setEndAt(LocalDateTime.now().plusDays("LIVE".equals(status) ? 3 : 7));
        listing.setInspectionAt(LocalDateTime.now().plusDays(1));
        listing.setStatus(status);
        listing.setEligibilityCriteria("Valid SMC Citizen ID. Business bidders should provide GST or trade license details where applicable.");
        listing.setTermsAndConditions("Bids are final. SMC may reject ineligible bids. Award is subject to document verification and payment compliance.");
        listing.setDocuments(List.of(new AuctionDocument("Auction Notice", "/documents/auction-notice.pdf", "PDF")));
        listing.setCreatedByOfficerId("SYSTEM");
        listing.setCreatedByOfficerName("System Seed");
        listing.setUpdatedAt(LocalDateTime.now());
        return listing;
    }
}
