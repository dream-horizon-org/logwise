# LogWise

Open-source end-to-end logging system for high-volume log processing. Streams logs from Vector → Kafka → Spark → S3/Athena, with Grafana dashboards and automated orchestration.

## 🚀 Quick Start

### Docker Setup (Recommended)

1. **Complete S3 & Athena Setup** (required first step):
   - Follow the [S3 & Athena Setup Guide](https://ds-horizon.github.io/logwise/setup-guides/self-host/s3-athena-setup)

2. **Run the setup script**:
   ```bash
   cd deploy
   ./setup.sh
   ```

3. **Access services**:
   - Grafana: `http://localhost:3000` (admin/admin)
   - Spark Master UI: `http://localhost:18080`
   - Orchestrator: `http://localhost:8080`

For detailed instructions, see the [Docker Setup Guide](https://ds-horizon.github.io/logwise/setup-guides/docker/).

## 📚 Documentation

Full documentation is available at: **[https://ds-horizon.github.io/logwise/](https://ds-horizon.github.io/logwise/)**

### Quick Links
- **[Docker Setup](https://ds-horizon.github.io/logwise/setup-guides/docker/)** - One-click Docker deployment
- **[Architecture Overview](https://ds-horizon.github.io/logwise/architecture-overview)** - System design and flow
- **[Component Guides](https://ds-horizon.github.io/logwise/components/vector)** - Detailed component documentation
- **[Self-Host Setup](https://ds-horizon.github.io/logwise/setup-guides/self-host/)** - Manual component setup

## 📁 Project Structure

```
logwise/
├── deploy/              # Docker deployment configuration
├── vector/              # Vector log collection config
├── spark/               # Spark streaming application
├── orchestrator/        # Spring Boot orchestrator service
└── docs/                # Documentation
```

## ✨ Features

- **High-throughput** log processing with Kafka streaming
- **Real-time dashboards** with Grafana integration
- **Scalable architecture** with Spark stream processing
- **Cost-efficient storage** using S3 and Athena
- **Production-ready** with automated orchestration
- **Docker support** for easy deployment

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 📧 Contact

For questions or support, please open an issue or contact the maintainers.
