import { printApi } from '@/api/services'
import { getOrCreateDeviceId } from '@/stores/authStore'
import { isApiSuccess, apiMessage, type JsonMap } from '@/api/client'

export interface CloudPrintPayload {
  documentType?: string
  documentId?: string
  text?: string
  printerId?: string | number
  idempotencyKey?: string
  [key: string]: unknown
}

/** Create a cloud print job for the given shop user. */
export async function createCloudPrintJob(
  userId: string,
  payload: CloudPrintPayload,
): Promise<JsonMap> {
  const fields: Record<string, string | number> = {
    userId,
    documentType: String(payload.documentType ?? 'RECEIPT'),
    documentId: String(payload.documentId ?? `web-${Date.now()}`),
    payload: JSON.stringify(
      payload.text != null ? { text: payload.text } : payload,
    ),
    idempotencyKey:
      String(payload.idempotencyKey ?? '') ||
      `web:${payload.documentType ?? 'RECEIPT'}:${Date.now()}`,
    android_device_id: getOrCreateDeviceId(),
  }
  if (payload.printerId != null && payload.printerId !== '') {
    fields.printerId = payload.printerId as string | number
  }

  const res = await printApi.createJob(fields)
  if (!isApiSuccess(res)) {
    throw new Error(apiMessage(res, 'Failed to create print job'))
  }
  return res
}
