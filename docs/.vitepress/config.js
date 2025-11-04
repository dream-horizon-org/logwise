import { defineConfig } from 'vitepress'

export default defineConfig({
    title: 'Logwise',
    description: 'Open-source end-to-end logging system capable of handling 15+ Gbps log throughput',

    // GitHub Pages config
    base: '/logwise/',

    themeConfig: {
        nav: [
            { text: 'Home', link: '/' },
            { text: 'Overview', link: '/what-is-logwise' },
            { text: 'Setup', link: '/setup/kafka' },
            { text: 'Components', link: '/components/vector' },
            { text: 'GitHub', link: 'https://github.com/ds-horizon/logwise' }
        ],

        sidebar: {
            '/': [
                {
                    text: 'Getting Started',
                    items: [
                        { text: 'Introduction', link: '/' },
                        { text: 'What is Logwise?', link: '/what-is-logwise' },
                        { text: 'Architecture Overview', link: '/architecture-overview' }
                    ]
                },
                {
                    text: 'Setup Guides',
                    items: [
                        {
                            text: 'Docker Logwise',
                            collapsed: true,
                            items: [
                                { text: 'Docker Setup Guide', link: '/setup/docker' }
                            ]
                        },
                        {
                            text: 'Self-Host Logwise',
                            collapsed: true,
                            items: [
                                { text: 'Vector', link: '/setup-guides/self-host/vector-setup' },
                                { text: 'Kafka + Kafka Manager', link: '/setup-guides/self-host/kafka-setup' },
                                { text: 'Spark', link: '/setup-guides/self-host/spark-setup' },
                                { text: 'Grafana', link: '/setup-guides/self-host/grafana-setup' },
                                { text: 'Orchestrator Service', link: '/setup-guides/self-host/orchestrator-service-setup' },
                            ]
                        }
                    ]
                },
                {
                    text: 'Send Logs',
                    items: [
                        { text: 'OpenTelemetry - EC2', link: '/send-logs/ec2/opentelemetry' },
                        { text: 'OpenTelemetry - Kubernetes', link: '/send-logs/kubernetes/opentelemetry' }
                    ]
                },
                {
                    text: 'Components',
                    items: [
                        { text: 'Vector', link: '/components/vector' },
                        { text: 'Kafka', link: '/components/kafka' },
                        { text: 'Apache Spark', link: '/components/spark' },
                        { text: 'Grafana', link: '/components/grafana' },
                        { text: 'Orchestrator Service', link: '/components/orchestrator' }
                    ]
                },
            ]
        },

        socialLinks: [
            { icon: 'github', link: 'https://github.com/ds-horizon/logwise' }
        ],

        footer: {
            message: 'Released under the MIT License.',
            copyright: 'Copyright © 2025 Logwise'
        },

        search: {
            provider: 'local'
        }
    }
})

