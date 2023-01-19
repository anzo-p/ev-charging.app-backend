## EV Charging - App Backend

### The main Application backend for an EV-Charging Charging Application

The project implements a Service that **functions as a backend to an Application through which to issue charging commands and observe charging history**. Users would issue commands to begin or stop charging by submitting these commands in a mobile app, or by presenting a physical token, an RFID token to the device or. These backends services would then communicate to each others via a Kinesis stream. The Outlet Backend -project ([link](https://github.com/anzo-p/ev-charging.outlet-backend)) works as backend to the Outlet Devices.

This Project works as playground for implementing services using Scala ZIO, into an architecture provisioned by AWS Serverless.

## 1 Main flow

1. Register Outlets  (using the [Outlet Backend](https://github.com/anzo-p/ev-charging.outlet-backend))
2. Register Customers
3. Issue charging commands

### 1.1 The State Machine and possible commands, ie. state transitions

- see OutletStateMachine in the project ev-charger-shared ([link](https://github.com/anzo-p/ev-charger-shared))
- see the REST API request handlers at `/app_backend/http/`
- see event handler at `/app_backend/events/`
- see respective event handlers in Outlet Backend ([link](https://github.com/anzo-p/ev-charging.outlet-backend)).

### 1.2 Issuing command to begin charging from Application UI
1. User connects the cable between their Vehicle and the Outlet Device
2. User submits request to begin charging int their Mobile Application
3. __App Backend forwards the request to Outlet Backend via Kinesis stream__
4. Outlet Backend verifies Outlet Device availability
5. Outlet Backend sends an event to SQS which commands the Outlet Device to begin charging.

Legends
- *Bold* = this service
- __Italics__ = I have not yet published The API Gateway project.

### 1.3 Charging by RFID token or stopping the charging with App or RFID tag

Further details of all these flows can be observed in
- the REST API request handlers at `/app_backend/http/`
- the event handler at `/appt_backend/events/`
- respective event handler in the Outlet Backend -project.

## 2 Requirements
- `ev-charger-shared`-library - obtain from [link](https://github.com/anzo-p/ev-charger-shared) and publish as locally available by running `sbt publishLocal`
- AWS Serverless resources - run Terraform script *.tf from [link](https://github.com/anzo-p/ev-charging.infra) to provision the required queues, streams, and tables.

## 3 Running

### 1. Run this service as well as the Outlet-Backend service

### 2. Register a Customer (User)

Make POST request to following url with json payload

`< domain, eg. localhost >:8080/api/customers`

```
{
    "address": "< customer address >",
    "email": "< whatever >",
    "paymentMethod": "< eg. VISA 0012 **** **** 0554 >"
}
```

### 3 Request to begin charging

Make POST request to following url with json payload

`< domain, eg. localhost >:8080/api/chargers/start`

```
{
    "customerId": < customerId from the registered customer above >,
    "outletId": < outletId from the registered outlet above >
}
```

### 4 Observe that the charging has begun in AWS resources

- DynamoDB table `ev-charging_charging-session_table"` has a new row which initially has the state `AppRequestsCharging` and soon updates to `Charging`
- DynamoDB table `ev-charging_charger-outlet_table` has the state `Charging`
- Kinesis stream `ev-charging_charging-events_stream` shows a roundtrip of events to and back from App Backend
- SQS queue `ev-charging_outlet-backend-to-device_queue` has a pollable message that commands the Outlet Device top begin charging.

### 5 Simulate a periodic Charging Report from the Device

Send the following message into SQS queue

```
{
    "outletId": < outletId from the registered outlet above >,
    "rfidTag": < rfidTag from the registerted user above >,
    "periodStart": "2023-23-03T10:15:30.123",
    "periodEnd": "2023-23-03T10:15:30.123",
    "outletStateChange": "DeviceRequestsCharging",
    "powerConsumption": 1.234
}
```

### 6 Observe accumulation of charging totals in AWS resources

- DynamoDB table `ev-charging_charger-outlet_table` has an increase in the field `powerConsumption`
- DynamoDB table `ev-charging_charging-session_table"` also has a similar increase in the field `powerConsumption`.

### 7 Issue further possible commands

..according to what is possible in the State machine and the event handles..
