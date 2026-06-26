package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.AuctionActionRequest;
import org.dcoffice.cachar.dto.AuctionBidRequest;
import org.dcoffice.cachar.dto.AuctionListingRequest;
import org.dcoffice.cachar.entity.AuctionAuditTrail;
import org.dcoffice.cachar.entity.AuctionBid;
import org.dcoffice.cachar.entity.AuctionListing;
import org.dcoffice.cachar.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping("/auctions")
    public ResponseEntity<ApiResponse<List<AuctionListing>>> publicListings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer wardNumber
    ) {
        return ResponseEntity.ok(ApiResponse.success("Auctions retrieved", auctionService.publicListings(status, category, wardNumber)));
    }

    @GetMapping("/auctions/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionListing>> publicDetail(@PathVariable String auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Auction retrieved", auctionService.publicDetail(auctionId)));
    }

    @GetMapping("/auctions/{auctionId}/bids/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bidSummary(@PathVariable String auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Bid summary retrieved", auctionService.bidSummary(auctionId)));
    }

    @PostMapping("/auctions/{auctionId}/bid")
    public ResponseEntity<ApiResponse<AuctionBid>> placeBid(
            @PathVariable String auctionId,
            @RequestBody AuctionBidRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Bid placed", auctionService.placeBid(auctionId, authentication.getName(), request)));
    }

    @GetMapping("/auctions/my-bids")
    public ResponseEntity<ApiResponse<List<AuctionBid>>> myBids(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("My bids retrieved", auctionService.myBids(authentication.getName())));
    }

    @PostMapping("/officer/auctions")
    public ResponseEntity<ApiResponse<AuctionListing>> create(@RequestBody AuctionListingRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Auction created", auctionService.createAuction(authentication.getName(), request)));
    }

    @PutMapping("/officer/auctions/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionListing>> update(@PathVariable String auctionId, @RequestBody AuctionListingRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Auction updated", auctionService.updateAuction(auctionId, authentication.getName(), request)));
    }

    @PostMapping("/officer/auctions/{auctionId}/publish")
    public ResponseEntity<ApiResponse<AuctionListing>> publish(@PathVariable String auctionId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Auction published", auctionService.publish(auctionId, authentication.getName())));
    }

    @PostMapping("/officer/auctions/{auctionId}/cancel")
    public ResponseEntity<ApiResponse<AuctionListing>> cancel(@PathVariable String auctionId, @RequestBody AuctionActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Auction cancelled", auctionService.cancel(auctionId, authentication.getName(), request)));
    }

    @PostMapping("/officer/auctions/{auctionId}/award")
    public ResponseEntity<ApiResponse<AuctionListing>> award(@PathVariable String auctionId, @RequestBody AuctionActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Auction awarded", auctionService.award(auctionId, authentication.getName(), request)));
    }

    @GetMapping("/officer/auctions")
    public ResponseEntity<ApiResponse<List<AuctionListing>>> officerAuctions() {
        return ResponseEntity.ok(ApiResponse.success("Officer auctions retrieved", auctionService.officerAuctions()));
    }

    @GetMapping("/officer/auctions/{auctionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> officerDetail(@PathVariable String auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Officer auction detail retrieved", auctionService.officerAuctionDetail(auctionId)));
    }

    @GetMapping("/officer/auctions/{auctionId}/bids")
    public ResponseEntity<ApiResponse<List<AuctionBid>>> officerBids(@PathVariable String auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Auction bids retrieved", auctionService.officerBids(auctionId)));
    }

    @GetMapping("/officer/auctions/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success("Auction dashboard retrieved", auctionService.dashboard()));
    }

    @GetMapping("/officer/auctions/{auctionId}/audit")
    public ResponseEntity<ApiResponse<List<AuctionAuditTrail>>> audit(@PathVariable String auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Auction audit retrieved", auctionService.auditTrail(auctionId)));
    }
}
