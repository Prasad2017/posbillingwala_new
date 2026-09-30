/* Controlled print-job lifecycle — prevents blind full-bill retries. */
enum PrintJobState {
  created,
  connecting,
  connected,
  sending,
  sent,
  cutting,
  completed,
  failed,
  unknownResult,
}

extension PrintJobStateX on PrintJobState {
  String get label {
    switch (this) {
      case PrintJobState.created:
        return 'Created';
      case PrintJobState.connecting:
        return 'Connecting';
      case PrintJobState.connected:
        return 'Connected';
      case PrintJobState.sending:
        return 'Sending';
      case PrintJobState.sent:
        return 'Sent';
      case PrintJobState.cutting:
        return 'Cutting';
      case PrintJobState.completed:
        return 'Completed';
      case PrintJobState.failed:
        return 'Failed';
      case PrintJobState.unknownResult:
        return 'Unknown';
    }
  }

  bool get isTerminal =>
      this == PrintJobState.completed ||
      this == PrintJobState.failed ||
      this == PrintJobState.unknownResult;
}

enum PrintErrorCode {
  none,
  printerNotFound,
  connectionTimeout,
  connectionRefused,
  invalidConfiguration,
  writeFailed,
  printerOffline,
  unsupportedOperation,
  bluetoothUnavailable,
  usbUnavailable,
  networkUnavailable,
  unknownResult,
  unknownError,
}

extension PrintErrorCodeX on PrintErrorCode {
  String get code {
    switch (this) {
      case PrintErrorCode.none:
        return 'NONE';
      case PrintErrorCode.printerNotFound:
        return 'PRINTER_NOT_FOUND';
      case PrintErrorCode.connectionTimeout:
        return 'CONNECTION_TIMEOUT';
      case PrintErrorCode.connectionRefused:
        return 'CONNECTION_REFUSED';
      case PrintErrorCode.invalidConfiguration:
        return 'INVALID_CONFIGURATION';
      case PrintErrorCode.writeFailed:
        return 'WRITE_FAILED';
      case PrintErrorCode.printerOffline:
        return 'PRINTER_OFFLINE';
      case PrintErrorCode.unsupportedOperation:
        return 'UNSUPPORTED_OPERATION';
      case PrintErrorCode.bluetoothUnavailable:
        return 'BLUETOOTH_UNAVAILABLE';
      case PrintErrorCode.usbUnavailable:
        return 'USB_UNAVAILABLE';
      case PrintErrorCode.networkUnavailable:
        return 'NETWORK_UNAVAILABLE';
      case PrintErrorCode.unknownResult:
        return 'UNKNOWN_RESULT';
      case PrintErrorCode.unknownError:
        return 'UNKNOWN_ERROR';
    }
  }

  String get userMessage {
    switch (this) {
      case PrintErrorCode.none:
        return '';
      case PrintErrorCode.printerNotFound:
        return 'Printer was not found. Check printer settings.';
      case PrintErrorCode.connectionTimeout:
        return 'Printer connection timed out.';
      case PrintErrorCode.connectionRefused:
        return 'Printer refused the connection.';
      case PrintErrorCode.invalidConfiguration:
        return 'Printer configuration is invalid.';
      case PrintErrorCode.writeFailed:
        return 'Could not send data to the printer.';
      case PrintErrorCode.printerOffline:
        return 'Printer appears to be offline.';
      case PrintErrorCode.unsupportedOperation:
        return 'This printer does not support that operation.';
      case PrintErrorCode.bluetoothUnavailable:
        return 'Bluetooth is unavailable.';
      case PrintErrorCode.usbUnavailable:
        return 'USB printer is unavailable.';
      case PrintErrorCode.networkUnavailable:
        return 'Network printer is unavailable.';
      case PrintErrorCode.unknownResult:
        return 'Printer status could not be confirmed. '
            'Please check the printer before retrying.';
      case PrintErrorCode.unknownError:
        return 'Printing failed. Please try again.';
    }
  }
}
