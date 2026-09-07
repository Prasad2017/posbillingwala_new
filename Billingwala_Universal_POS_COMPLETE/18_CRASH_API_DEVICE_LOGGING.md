# 18 Crash Api Device Logging

## Pipeline
Observability (Crashlytics/Perf) → ErrorLogQueue → Uploader/Worker → Admin inbox.

## Facade
`Extra/CrashApiDeviceLogging.java`

## Live classes
`Observability`, `ErrorLogReporter`, `ErrorLogQueue`, `ErrorLogUploader`, `ErrorLogFlushWorker`, `DeviceHealthMonitor`.
