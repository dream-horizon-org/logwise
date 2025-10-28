# Getting Started

Welcome to logwise! This guide will help you get started quickly.

## Installation

### Quick Start with Docker

```bash
docker run -d -p 8080:8080 logwise/logwise:latest
```

### From Source

```bash
git clone https://github.com/your-org/logwise.git
cd logwise
./build.sh
```

## Usage

Send your first logs:

```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, logwise!"}'
```

## Next Steps

- Check out the API documentation
- Explore the dashboard at http://localhost:8080
- Read about scaling for production
