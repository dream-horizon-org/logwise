import { defineConfig } from 'vitepress'
import { readFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

// Get current directory in ES modules
const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// Read version from VERSION file
function getVersion() {
    try {
        // VitePress config is in docs/.vitepress/, VERSION file is in docs/
        const versionFile = join(__dirname, '..', 'VERSION')
        return readFileSync(versionFile, 'utf-8').trim()
    } catch (error) {
        // Fallback to package.json if VERSION file doesn't exist
        try {
            const packageJson = JSON.parse(readFileSync(join(__dirname, '..', 'package.json'), 'utf-8'))
            return packageJson.version || '0.0.1'
        } catch {
            return '0.0.1'
        }
    }
}

const version = getVersion()
const isSnapshot = version.includes('-SNAPSHOT')
const releaseVersion = version.replace('-SNAPSHOT', '')

export default defineConfig({
    title: 'Logwise',
    description: 'Open-source, cost-effective end-to-end logging system featuring full architecture, deployment automation, dashboards, and production-ready scaling guides',
    site: 'https://ds-horizon.github.io/logwise/',
    // GitHub Pages config
    base: '/logwise/',
    head: [
        [
            'script',
            { async: '', src: 'https://www.googletagmanager.com/gtag/js?id=G-R20W1243YJ' }
        ],
        [
            'script',
            {},
            `window.dataLayer = window.dataLayer || [];
                function gtag(){dataLayer.push(arguments);}
                gtag('js', new Date());
                gtag('config', 'G-R20W1243YJ');`
        ],
        ['style', {}, `
            :root {
                --vp-c-brand-1: #5FD3E0;
                --vp-c-brand-2: #3ABEB2;
                --vp-c-brand-3: #1A6A74;
            }
        `],
        ['link', { rel: 'icon', href: '/logwise/logo1.png' }]
    ],
    themeConfig: {
        logo: '/logo1.png',
        nav: [
            { text: 'Home', link: '/' },
            { text: 'Overview', link: '/what-is-logwise' },
            { text: 'Roadmap', link: '/roadmap' },
            { text: 'Setup', link: '/setup-guides/docker' },
            {
                text: `v${version}`, items: isSnapshot ? [
                    { text: 'Latest Release', link: 'https://github.com/ds-horizon/logwise/releases/latest' },
                    { text: 'All Releases', link: 'https://github.com/ds-horizon/logwise/releases' }
                ] : [
                    { text: 'Release Notes', link: `https://github.com/ds-horizon/logwise/releases/tag/v${releaseVersion}` },
                    { text: 'All Releases', link: 'https://github.com/ds-horizon/logwise/releases' }
                ]
            },
            { text: 'GitHub', link: 'https://github.com/ds-horizon/logwise' }
        ],

        sidebar: {
            '/': [
                {
                    text: 'Getting Started',
                    items: [
                        { text: 'Introduction', link: '/' },
                        { text: 'What is Logwise?', link: '/what-is-logwise' },
                        { text: 'Roadmap', link: '/roadmap' },
                        {
                            text: 'Architecture Overview',
                            link: '/architecture-overview',
                            collapsed: true,
                            items: [
                                { text: 'Vector', link: '/components/vector' },
                                { text: 'Kafka', link: '/components/kafka' },
                                { text: 'S3 + Athena', link: '/components/s3-athena' },
                                { text: 'Apache Spark', link: '/components/spark' },
                                { text: 'Grafana', link: '/components/grafana' },
                                { text: 'Orchestrator Service', link: '/components/orchestrator' }
                            ]
                        }
                    ]
                },
                {
                    text: 'Setup Guides',
                    items: [
                        {
                            text: 'Docker Logwise',
                            collapsed: true,
                            items: [
                                { text: 'Docker Setup Guide', link: '/setup-guides/docker/index' }
                            ]
                        },
                        {
                            text: 'Self-Host Logwise',
                            collapsed: true,
                            items: [
                                { text: 'Kafka', link: '/setup-guides/self-host/kafka-setup' },
                                { text: 'Vector', link: '/setup-guides/self-host/vector-setup' },
                                { text: 'S3 + Athena', link: '/setup-guides/self-host/s3-athena-setup' },
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
            ]
        },

        socialLinks: [
            { icon: 'github', link: 'https://github.com/ds-horizon/logwise' }
        ],

        footer: {
            message: `Released under the MIT License. Version ${version}`,
            copyright: 'Copyright © 2025 Logwise'
        },

        search: {
            provider: 'local'
        }
    }
})

