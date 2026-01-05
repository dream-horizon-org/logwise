{{- define "logwise.namespace" -}}
{{- default .Values.global.namespace .Release.Namespace -}}
{{- end -}}


