import { defineConfig } from 'vitepress'

export default defineConfig({
    title: 'Logwise',
    description: 'Open-source end-to-end logging system capable of handling 15+ Gbps log throughput',

    // GitHub Pages config
    base: '/logwise/',

    themeConfig: {
        nav: [
            { text: 'Home', link: '/' },
            { text: 'Architecture', link: '/architecture-overview' },
            { text: 'Components', link: '/components/vector' },
            { text: 'Setup', link: '/setup/kafka' },
            { text: 'GitHub', link: 'https://github.com/ds-horizon/logwise' }
        ],

        sidebar: {
            '/': [
                {
                    text: 'Getting Started',
                    items: [
                        { text: 'Introduction', link: '/' }
                    ]
                },
                {
                    text: 'Architecture',
                    items: [
                        { text: 'Architecture Overview', link: '/architecture-overview' }
                    ]
                },
                {
                    text: 'Components',
                    items: [
                        { text: 'Vector', link: '/components/vector' },
                        { text: 'Kafka', link: '/components/kafka' },
                        { text: 'Kafka Manager', link: '/components/kafka-manager' },
                        { text: 'Apache Spark', link: '/components/spark' },
                        { text: 'Grafana', link: '/components/grafana' },
                        { text: 'Orchestrator Service', link: '/components/orchestrator' }
                    ]
                },
                {
                    text: 'Setup Guides',
                    items: [
                        { text: 'Kafka Setup', link: '/setup/kafka' }
                    ]
                }
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

