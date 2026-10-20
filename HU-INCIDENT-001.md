# HU-INCIDENT-001: Driver Reports Incident

## Description
The driver can report an incident or emergency during a trip. The system automatically notifies the school administrators and the guardians of students on board.

## Related Requirements
- **FR-8.1**: Report an incident/emergency during a trip with type and optional description
- **FR-8.2**: Automatically notify the school and the guardians of the students on board
- **HU-INCIDENT-001**: Driver reports incident during trip

## Implementation

### Backend (ms-notification)
The `ReportIncidentService` implements the Command pattern and publishes events via Kafka:

- **Service**: `ReportIncidentService` implements `IReportIncidentUseCase`
- **Endpoint**: `POST api/v1/notifications/incidents`
- **Kafka Topic**: `incident.reported`
- **Event**: `IncidentReportedEvent` published with alert details and recipients

### Key Features
1. Driver reports incident with alert type (from catalog) and optional description
2. System validates active trip, incident type from catalog
3. Resolves recipients: school admins + parents of students on board
4. Persists `Alert` and `AlertRecipient` entities
5. Publishes `incident.reported` Kafka event
6. Frontend can consume notifications via `GET api/v1/notifications/guardian`

### Command Pattern
The service follows the Command pattern:
- `ExecuteAsync(driverProfileId, request, ct)` is the command method
- Validates business rules (active trip, valid type, description length)
- Persists data and publishes domain event
- Returns DTO with response data

### Kafka Integration
- Event published to `incident.reported` topic
- Consumed by `IncidentReportedConsumer`
- Triggers push notifications to guardians' devices
- Best-effort delivery: alerts persist in DB even if push fails

### API Contract
```
POST /api/v1/notifications/incidents
{
  "alertTypeId": 21,  // From AlertType catalog
  "description": "Optional incident description"
}
```

### Response
```json
{
  "alertId": "guid",
  "routeExecutionId": "guid",
  "studentsOnBoard": 2,
  "recipientsNotified": 3
}
```
