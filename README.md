# sg-ms-iam

Identity and Access Management microservice for School Guardian. Handles login, session refresh persistence, and role-based access for parents, students, drivers, and administrators. Built with Java 21 and Maven.

## Getting Started

These instructions will give you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on deploying the project on a live system.

### Prerequisites

Requirements for the software and other tools to build, test and push:

- Java 21
- Maven 3.9
- Docker
- Docker Compose
- The `sg-services-network` Docker network (external, shared across services)

### Installing

A step by step series of examples that tell you how to get a development environment running.

Create the external network if it does not exist:

    docker network create sg-services-network

Start the service:

    docker compose up -d

The service exposes HTTP on port 8080 and gRPC on port 5001. Environment variables are loaded from `.env`, including `APP_INTERNAL_API_KEY` which is required.

## Running the tests

Explain how to run the automated tests for this system.

### Sample Tests

Tests are executed against the test service that runs in Docker Compose for each service.

### Style test

Checks if the best practices and the right coding style has been used, validated through the test service in Docker Compose.

## Deployment

Deployment is performed via GitHub Actions to an AWS EC2 instance.

## Built With

- Java 21
- Maven 3.9
- Docker Compose — Local development stack

## Contributing

Please read [CONTRIBUTING.md](https://github.com/school-guardian-project/.github/blob/main/CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the SCHOOL GUARDIAN PROPRIETARY SOFTWARE LICENSE Version 1.0 — 2026. See the [LICENSE](LICENSE) file for details.
