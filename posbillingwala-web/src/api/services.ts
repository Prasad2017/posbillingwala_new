import { Endpoints } from './endpoints'
import {
  apiMessage,
  getQuery,
  isApiSuccess,
  listFrom,
  postForm,
  type JsonMap,
} from './client'
import { ResponseKeys } from './endpoints'

export type LoginStatus = '0' | '1' | '2' | '3' | '4' | string

export interface LoginPayload {
  status: LoginStatus
  message?: string
  mpin?: string
  licenceId?: string
  ownerId?: string
  userName?: string
  shopName?: string
  shopImage?: string
  licenceKey?: string
  licenceKeyRegDate?: string
  licenceKeyExpireDate?: string
  fastBilling?: string
  takeAway?: string
  dineIn?: string
  mess?: string
  reportPin?: string
  totalSaleData?: string
  todaySaleData?: string
  licenseType?: string
  isTrial?: string
  trialDays?: string
  trialMaxBills?: string
  trialBillCount?: string
  trialBillsRemaining?: string
  authToken?: string
  tokenExpiresAt?: string
  licensePayload?: string
  licenseSignature?: string
  organizationId?: string
  branchId?: string
  branchLabel?: string
  userManagementEnabled?: string
  maxUsers?: string
  maxDevices?: string
  maxPrinters?: string
  permissionVersion?: string
}

function s(v: unknown): string | undefined {
  if (v == null) return undefined
  const t = String(v)
  return t.length ? t : undefined
}

export function parseLogin(data: JsonMap): LoginPayload {
  return {
    status: s(data.status) ?? '0',
    message: s(data.message),
    mpin: s(data.mpin),
    licenceId: s(data.licenceId),
    ownerId: s(data.ownerId),
    userName: s(data.userName),
    shopName: s(data.shopName),
    shopImage: s(data.shopImage),
    licenceKey: s(data.licenceKey),
    licenceKeyRegDate: s(data.licence_key_reg_date),
    licenceKeyExpireDate: s(data.licence_key_expire_date),
    fastBilling: s(data.fastBilling),
    takeAway: s(data.takeAway),
    dineIn: s(data.dineIn),
    mess: s(data.mess),
    reportPin: s(data.reportPin),
    totalSaleData: s(data.totalSaleData),
    todaySaleData: s(data.todaySaleData),
    licenseType: s(data.licenseType),
    isTrial: s(data.isTrial),
    trialDays: s(data.trialDays),
    trialMaxBills: s(data.trialMaxBills),
    trialBillCount: s(data.trialBillCount),
    trialBillsRemaining: s(data.trialBillsRemaining),
    authToken: s(data.authToken),
    tokenExpiresAt: s(data.tokenExpiresAt),
    licensePayload: s(data.licensePayload),
    licenseSignature: s(data.licenseSignature),
    organizationId: s(data.organizationId),
    branchId: s(data.branchId),
    branchLabel: s(data.branchLabel),
    userManagementEnabled: s(data.userManagementEnabled),
    maxUsers: s(data.maxUsers),
    maxDevices: s(data.maxDevices),
    maxPrinters: s(data.maxPrinters),
    permissionVersion: s(data.permissionVersion),
  }
}

export const authApi = {
  async loginCheck(licenceKey: string, deviceId: string) {
    const data = await postForm(Endpoints.login, {
      app_licence_key: licenceKey,
      android_device_id: deviceId,
    })
    return parseLogin(data)
  },

  async loginMpin(fields: {
    mpin: string
    licenceKey: string
    deviceId: string
    deviceName: string
  }) {
    const data = await postForm(Endpoints.loginMpin, {
      mpin: fields.mpin,
      app_licence_key: fields.licenceKey,
      android_device_id: fields.deviceId,
      androidId: fields.deviceId,
      device_name: fields.deviceName,
    })
    return parseLogin(data)
  },

  async checkLicenceExpire(userId: string, deviceId: string, deviceName: string) {
    const data = await postForm(Endpoints.checkLicenceExpire, {
      userId,
      android_device_id: deviceId,
      device_name: deviceName,
    })
    return parseLogin(data)
  },

  async refreshAuthToken(licenceKey: string, deviceId: string) {
    const data = await postForm(Endpoints.refreshAuthToken, {
      app_licence_key: licenceKey,
      android_device_id: deviceId,
    })
    if (!isApiSuccess(data)) return null
    const token = s(data.authToken)
    if (!token) return null
    return {
      token,
      expiresAt: s(data.tokenExpiresAt) ?? null,
    }
  },

  async registerTrial(fields: {
    name: string
    contactNumber: string
    address: string
    shopName: string
  }) {
    const data = await postForm(Endpoints.registerTrial, {
      name: fields.name,
      contact_number: fields.contactNumber,
      address: fields.address,
      shopName: fields.shopName,
    })
    return {
      ok: isApiSuccess(data),
      message: apiMessage(data),
      raw: data,
      licenceKey: s(data.licenceKey) ?? s(data.app_licence_key),
      mpin: s(data.mpin),
      reportPin: s(data.reportPin),
    }
  },

  async staffLogin(fields: {
    licenceKey: string
    mobileNumber: string
    pin: string
    deviceId: string
  }) {
    return postForm(Endpoints.staffLogin, {
      app_licence_key: fields.licenceKey,
      userId: fields.licenceKey,
      mobileNumber: fields.mobileNumber,
      appLoginPin: fields.pin,
      android_device_id: fields.deviceId,
    })
  },

  async logout(licenceKey: string) {
    return getQuery(Endpoints.logOut, { licenceKey })
  },
}

export const catalogApi = {
  getFoodTypes: (userId: string) =>
    getQuery(Endpoints.getFoodTypeList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.foodTypeResponse),
    ),
  getCategories: (userId: string) =>
    getQuery(Endpoints.getCategoryList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.categoryResponse),
    ),
  getSubcategories: (userId: string) =>
    getQuery(Endpoints.getSubcategoryList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.subcategoryResponse),
    ),
  getProducts: (userId: string) =>
    getQuery(Endpoints.getProductList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.productResponse),
    ),
  getPortions: (userId: string) =>
    getQuery(Endpoints.getPortionList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.portionResponse),
    ),
  getPortionMasters: (userId: string) =>
    getQuery(Endpoints.getPortionMasterList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.portionMasterResponse),
    ),
  getCombos: (userId: string) =>
    getQuery(Endpoints.getComboList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.comboResponse),
    ),
  getComboItems: (userId: string) =>
    getQuery(Endpoints.getComboItemList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.comboItemResponse),
    ),
  insertCategory: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertCategory, fields),
  insertSubcategory: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertSubcategory, fields),
  insertProduct: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertProduct, fields),
  insertPortion: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertPortion, fields),
  insertPortionMaster: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertPortionMaster, fields),
  insertCombo: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertCombo, fields),
  insertComboItem: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertComboItem, fields),
}

export const companyApi = {
  getCompanyList: (userId: string) =>
    getQuery(Endpoints.getCompanyList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.companyResponse),
    ),
  insertCompanyDetail: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertCompanyDetail, fields),
  getHomeSalesOverview: (userId: string) =>
    getQuery(Endpoints.getHomeSalesOverview, { userId }),
}

export const invoiceApi = {
  insertInvoice: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertInvoice, fields),
  insertInvoiceProduct: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertInvoiceProduct, fields),
  insertInvoiceComboItem: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertInvoiceComboItem, fields),
  getInvoiceList: (userId: string) =>
    getQuery(Endpoints.getInvoiceList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.invoiceResponse),
    ),
  getInvoiceProducts: (userId: string, invoiceNetworkStatus?: string) =>
    getQuery(Endpoints.getInvoiceProductList, {
      userId,
      invoiceNetworkStatus,
    }).then((d) => listFrom(d, ResponseKeys.invoiceProductResponse)),
  getPosSalesReport: (userId: string, fromDate?: string, toDate?: string) =>
    getQuery(Endpoints.getPosSalesReport, { userId, fromDate, toDate }),
}

export const tablesApi = {
  getTables: (userId: string) =>
    getQuery(Endpoints.getPosTableList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.posTableResponse),
    ),
  getDiningAreas: (userId: string) =>
    getQuery(Endpoints.getDiningAreaList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.diningAreaResponse),
    ),
  getTableTypes: (userId: string) =>
    getQuery(Endpoints.getTableTypeList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.tableTypeResponse),
    ),
  getDiningSessions: (userId: string) =>
    getQuery(Endpoints.getDiningSessionList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.diningSessionResponse),
    ),
  insertDiningSession: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertDiningSession, fields),
  insertPosTable: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertPosTable, fields),
  insertDiningArea: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertDiningArea, fields),
  insertTableType: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertTableType, fields),
}

export const messApi = {
  getMembers: (userId: string) =>
    getQuery(Endpoints.getMessMemberList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.memberResponse),
    ),
  insertMember: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertMessMember, fields),
  getTokens: (userId: string) =>
    getQuery(Endpoints.getMessTokenList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.messTokenResponse),
    ),
  insertToken: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertMessToken, fields),
  verifyToken: (fields: Record<string, string | number>) =>
    postForm(Endpoints.verifyMessToken, fields),
  getPayments: (userId: string) =>
    getQuery(Endpoints.getMessMemberPaymentList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.messPaymentResponse),
    ),
  insertPayment: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertMessPayment, fields),
  getInvoices: (userId: string) =>
    getQuery(Endpoints.getMessInvoiceList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.messInvoiceResponse),
    ),
  insertInvoice: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertMessInvoice, fields),
  getCommonQr: (userId: string) => getQuery(Endpoints.messQrGet, { userId }),
  generateCommonQr: (userId: string) =>
    postForm(Endpoints.messQrGenerate, { userId }),
  getMealSessions: (userId: string) =>
    getQuery(Endpoints.messMealSessionList, { userId }),
  saveMealSession: (fields: Record<string, string | number>) =>
    postForm(Endpoints.messMealSessionSave, fields),
  getTodayTokens: (userId: string) =>
    getQuery(Endpoints.messMealTokenToday, { userId }),
  getShopSetting: (userId: string) =>
    getQuery(Endpoints.messShopSettingGet, { userId }),
  saveShopSetting: (fields: Record<string, string | number>) =>
    postForm(Endpoints.messShopSettingSave, fields),
}

export const inventoryApi = {
  getInventory: (userId: string) =>
    getQuery(Endpoints.getInventoryList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.inventoryResponse),
    ),
  insertInventory: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertInventory, fields),
  getExpenses: (userId: string) =>
    getQuery(Endpoints.getExpensesList, { userId }).then((d) =>
      listFrom(d, ResponseKeys.expensesResponse),
    ),
  insertExpense: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertExpenses, fields),
}

export const staffApi = {
  getList: (userId: string) => getQuery(Endpoints.getStaffList, { userId }),
  getOne: (userId: string, staffId: string) =>
    getQuery(Endpoints.getStaff, { userId, staffId }),
  insert: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertStaff, fields),
  update: (fields: Record<string, string | number>) =>
    postForm(Endpoints.updateStaff, fields),
  deactivate: (fields: Record<string, string | number>) =>
    postForm(Endpoints.deactivateStaff, fields),
  resetPin: (fields: Record<string, string | number>) =>
    postForm(Endpoints.resetStaffPin, fields),
  getRoleDefaults: (userId: string) =>
    getQuery(Endpoints.getRoleDefaults, { userId }),
  getEffectivePermissions: (userId: string, staffId: string) =>
    getQuery(Endpoints.getEffectivePermissions, { userId, staffId }),
  getSalaryList: (userId: string) =>
    getQuery(Endpoints.getSalaryList, { userId }),
  saveSalaryPayment: (fields: Record<string, string | number>) =>
    postForm(Endpoints.saveSalaryPayment, fields),
}

export const supportApi = {
  getTickets: (userId: string) =>
    getQuery(Endpoints.getSupportTickets, { userId }).then((d) =>
      listFrom(d, ResponseKeys.tickets),
    ),
  createTicket: (fields: Record<string, string | number>) =>
    postForm(Endpoints.createSupportTicket, fields),
  getTicketDetails: (userId: string, ticketId: string) =>
    getQuery(Endpoints.getSupportTicketDetails, { userId, ticketId }),
  replyTicket: (fields: Record<string, string | number>) =>
    postForm(Endpoints.replySupportTicket, fields),
}

export const printApi = {
  createJob: (fields: Record<string, string | number>) =>
    postForm(Endpoints.createPrintJob, fields),
  getJobs: (userId: string) =>
    getQuery(Endpoints.getPrintJobList, { userId }),
  acknowledge: (fields: Record<string, string | number>) =>
    postForm(Endpoints.acknowledgePrintJob, fields),
  retry: (fields: Record<string, string | number>) =>
    postForm(Endpoints.retryPrintJob, fields),
  getStorePrinters: (userId: string) =>
    getQuery(Endpoints.getStorePrinterList, { userId }),
  insertStorePrinter: (fields: Record<string, string | number>) =>
    postForm(Endpoints.insertStorePrinter, fields),
}

export const deviceApi = {
  getList: (userId: string) => getQuery(Endpoints.getPosDeviceList, { userId }),
  register: (fields: Record<string, string | number>) =>
    postForm(Endpoints.registerPosDevice, fields),
  revoke: (fields: Record<string, string | number>) =>
    postForm(Endpoints.revokePosDevice, fields),
}
