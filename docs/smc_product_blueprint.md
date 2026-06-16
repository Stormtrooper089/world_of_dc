# Silchar Municipal Corporation Digital Governance Blueprint

## Product Direction

The platform should evolve as a modular municipal operating system, not only a complaint portal. Existing grievance, ward, SLA, officer board, task board and tracking capabilities should become the foundation for service delivery, inspection, revenue monitoring and commissioner-level accountability.

## Feature Roadmap

### Must-Have MVP

| Module | Problem Solved | Roles | Data Fields | Workflow | APIs | UI |
| --- | --- | --- | --- | --- | --- | --- |
| Citizen Grievance and SLA Tracking | Citizens need one tracking ID and officers need SLA visibility. | Citizen, Ward Officer, Department Officer, Admin | complaintNumber, category, wardNumber, geo, photos, status, slaDueAt, rating | Citizen files, officer assigns, staff updates, SLA breach appears in dashboard, citizen rates closure | `POST /api/complaints/create`, `GET /api/complaints/track/{number}`, `PUT /api/complaints/update` | Citizen form, track modal, officer complaint board |
| Ward Governance Dashboard | Commissioner needs ward-wise pendency and SLA risk. | Admin, Commissioner, Ward Officer | wardNumber, category, status, slaDueAt, citizenRating | System aggregates complaints by ward/category/status | `GET /api/governance/dashboard?days=30` | Governance Dashboard |
| Field Inspection Evidence | Closure must be backed by geo-tag, timestamp, photo, remarks. | Field Staff, Ward Officer | complaintId, latitude, longitude, photo, remarks, inspectedAt | Field staff visits, uploads proof, officer reviews | Future `POST /api/field-inspections` | Mobile inspection form |
| Solid Waste Monitoring | Missed garbage collection needs ward/route tracking. | Citizen, Field Staff, Sanitation Officer | routeId, ward, collectionDate, status, vehicle, photo | Staff marks collection; citizens report missed pickup | Future `POST /api/waste-routes/{id}/visits` | Waste collection board |
| Feedback and Rating | Closure quality needs citizen validation. | Citizen, Admin | complaintNumber, rating, feedback, confirmedResolved | Citizen rates after resolution; low ratings reopen/escalate | Existing complaint feedback fields; future dedicated API | Citizen track modal |

### Phase 2

| Module | Problem Solved | Key Additions |
| --- | --- | --- |
| Public Works and Contractor Tracker | Tracks work orders, milestones, contractor accountability | workOrder, contractorId, ward, budget, milestones, geo photos |
| Hoarding and Advertisement Revenue | Prevents revenue leakage from hoardings/screens | assetId, owner, GPS, permit, feeDue, expiry, paymentStatus |
| Public Asset Registry | Maps toilets, streetlights, garbage points, drains, markets | assetType, GPS, condition, maintainer, lastInspection |
| Escalation Matrix | SLA breach should automatically move up hierarchy | category, department, firstOfficer, escalationOfficer, breachHours |
| Reports Export | Officers need Excel/PDF for meetings | ward report, SLA breach report, category hotspot report |

### Advanced / AI-Enabled

| Module | Use |
| --- | --- |
| Duplicate Complaint Detection | Cluster repeated complaints by ward, location and text similarity |
| Predictive Waterlogging Alerts | Combine complaint patterns, rainfall feeds and drain hotspots |
| WhatsApp Chatbot | File and track grievances without installing an app |
| GIS Heatmap | Ward/category/SLA heat layers for commissioner reviews |
| AI Triage | Suggest category, ward and priority from text/photo |

## Database Design

MongoDB remains suitable because complaints, inspections, attachments and asset records vary by module.

### Existing Collection: `complaints`

Already supports the MVP: `complaintNumber`, `category`, `priority`, `status`, `location`, `latitude`, `longitude`, `wardNumber`, `wardName`, `zone`, `slaDueAt`, `citizenRating`, `citizenFeedback`, `assignedDepartment`, `assignedToId`, `createdAt`, `updatedAt`, `closedAt`, `documents`, `history`.

Recommended indexes:

- `complaintNumber` unique
- `wardNumber`
- `category`
- `status`
- `slaDueAt`
- `assignedToId`
- `createdAt`

### Future Collection: `field_inspections`

Fields: `inspectionId`, `complaintId`, `complaintNumber`, `inspectionType`, `inspectedById`, `wardNumber`, `latitude`, `longitude`, `photoUrls`, `remarks`, `result`, `inspectedAt`, `createdAt`.

### Future Collection: `municipal_assets`

Fields: `assetId`, `assetType`, `name`, `wardNumber`, `zone`, `latitude`, `longitude`, `condition`, `status`, `maintainerId`, `lastInspectionAt`, `metadata`.

### Future Collection: `work_orders`

Fields: `workOrderId`, `title`, `contractorId`, `wardNumber`, `budget`, `startDate`, `dueDate`, `status`, `progressPercent`, `milestones`, `photos`, `payments`.

### Future Collection: `advertisement_assets`

Fields: `assetId`, `type`, `ownerName`, `location`, `wardNumber`, `latitude`, `longitude`, `permitNumber`, `feeDue`, `feePaid`, `expiryDate`, `status`.

## API Design

Current MVP API added:

- `GET /api/governance/dashboard?days=30`
  - Returns summary metrics, ward performance, category performance, status breakdown and priority watchlist.

Future APIs:

- `POST /api/field-inspections`
- `GET /api/field-inspections?complaintId={id}`
- `POST /api/waste/collections`
- `GET /api/waste/dashboard?wardNumber={ward}`
- `POST /api/work-orders`
- `PUT /api/work-orders/{id}/progress`
- `GET /api/advertisements/dashboard`
- `POST /api/complaints/{id}/feedback`
- `GET /api/reports/governance.xlsx`

## UI Wireframe Structure

Citizen:

- Home
- File Grievance
- Track Grievance
- Feedback after resolution
- Public asset/service information

Officer:

- Complaint Board
- Task Board
- Governance Dashboard
- Squad Tracking
- Squad Management
- Profile

Admin/Commissioner:

- Ward-wise pendency
- SLA breach heatlist
- Category hotspots
- Officer/department performance
- Export reports

Field Staff:

- Mobile task list
- Start inspection
- Capture geo/photo evidence
- Submit remarks

Revenue:

- Hoarding registry
- Permit expiry
- Fee due/paid
- Ward-wise revenue leakage

## Implementation Priority

1. Governance Dashboard using existing complaint records.
2. Field Inspection evidence linked to complaint status updates.
3. Citizen feedback/rating submission flow.
4. Solid waste route visits and missed pickup reporting.
5. Work order/contractor milestone tracking.
6. Hoarding asset and revenue dashboard.
