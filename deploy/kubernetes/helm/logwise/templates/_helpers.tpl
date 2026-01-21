{{- define "logwise.namespace" -}}
{{- default .Values.global.namespace .Release.Namespace -}}
{{- end -}}

{{/*
Construct full image name with registry
Usage: {{ include "logwise.image" (dict "root" . "repository" "logwise-orchestrator" "tag" "latest") }}
*/}}
{{- define "logwise.image" -}}
{{- $registry := .root.Values.global.imageRegistry -}}
{{- $repository := .repository -}}
{{- $tag := .tag | default "latest" -}}
{{- if and $registry (not (contains "/" $repository)) -}}
{{- printf "%s/%s:%s" $registry $repository $tag -}}
{{- else -}}
{{- printf "%s:%s" $repository $tag -}}
{{- end -}}
{{- end -}}

{{/*
Standard labels for all resources
*/}}
{{- define "logwise.labels" -}}
app.kubernetes.io/name: {{ include "logwise.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels for deployments/statefulsets
*/}}
{{- define "logwise.selectorLabels" -}}
app.kubernetes.io/name: {{ include "logwise.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Chart name
*/}}
{{- define "logwise.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Chart full name
*/}}
{{- define "logwise.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

