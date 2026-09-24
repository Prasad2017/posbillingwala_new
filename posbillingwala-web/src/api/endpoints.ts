export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() ||
  'https://posbillingwala.com/androidApp/'

export const MEDIA_BASE_URL =
  import.meta.env.VITE_MEDIA_BASE_URL?.trim() ||
  'https://posbillingwala.com/storage/app/'

export function mediaUrl(path?: string | null): string | null {
  const p = path?.trim() ?? ''
  if (!p) return null
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  const base = MEDIA_BASE_URL.endsWith('/') ? MEDIA_BASE_URL : `${MEDIA_BASE_URL}/`
  return `${base}${p.startsWith('/') ? p.slice(1) : p}`
}

export const Endpoints = {
  login: 'Login.php',
  loginMpin: 'LoginMpin.php',
  updateMpin: 'updateMPin.php',
  updateAndroidKey: 'updateAndroidKey.php',
  checkLicenceExpire: 'check_licence_expire.php',
  registerTrial: 'registerTrial.php',
  logOut: 'LogOut.php',
  refreshAuthToken: 'refreshAuthToken.php',
  staffLogin: 'staffLogin.php',

  getFoodTypeList: 'getFoodTypeList.php',
  getCategoryList: 'getCategoryList.php',
  getSubcategoryList: 'getSubcategoryList.php',
  getProductList: 'getProductList.php',
  getPortionList: 'getPortionList.php',
  getPortionMasterList: 'getPortionMasterList.php',
  getComboList: 'getComboList.php',
  getComboItemList: 'getComboItemList.php',
  insertCategory: 'insertCategory.php',
  insertSubcategory: 'insertSubcategory.php',
  insertProduct: 'insertProduct.php',
  insertPortion: 'insertPortion.php',
  insertPortionMaster: 'insertPortionMaster.php',
  insertCombo: 'insertCombo.php',
  insertComboItem: 'insertComboItem.php',

  getCompanyList: 'getCompanyList.php',
  insertCompanyDetail: 'insertCompanyDetail.php',
  getHomeSalesOverview: 'getHomeSalesOverview.php',

  getPosTableList: 'getPosTableList.php',
  insertPosTable: 'insertPosTable.php',
  getDiningAreaList: 'getDiningAreaList.php',
  insertDiningArea: 'insertDiningArea.php',
  getTableTypeList: 'getTableTypeList.php',
  insertTableType: 'insertTableType.php',
  getDiningSessionList: 'getDiningSessionList.php',
  insertDiningSession: 'insertDiningSession.php',

  insertInvoice: 'insertInvoice.php',
  insertInvoiceProduct: 'insertInvoiceProduct.php',
  insertInvoiceComboItem: 'insertInvoiceComboItem.php',
  deleteInvoiceProduct: 'deleteInvoiceProduct.php',
  getInvoiceList: 'getInvoiceList.php',
  getInvoiceProductList: 'getInvoiceProductList.php',
  getInvoiceComboItemList: 'getInvoiceComboItemList.php',
  getPosSalesReport: 'getPosSalesReport.php',

  insertInventory: 'insertInventory.php',
  getInventoryList: 'getInventoryList.php',
  insertExpenses: 'insertExpenses.php',
  getExpensesList: 'getExpensesList.php',

  getMessMemberList: 'getMessMemberList.php',
  insertMessMember: 'insertMessMember.php',
  getMessMemberPaymentList: 'getMessMemberPaymentList.php',
  insertMessPayment: 'insertMessPayment.php',
  getMessInvoiceList: 'getMessInvoiceList.php',
  insertMessInvoice: 'insertMessInvoice.php',
  getMessTokenList: 'getMessTokenList.php',
  insertMessToken: 'insertMessToken.php',
  verifyMessToken: 'verifyMessToken.php',
  messQrGet: 'mess_qr_get.php',
  messQrGenerate: 'mess_qr_generate.php',
  messMealSessionList: 'mess_meal_session_list.php',
  messMealSessionSave: 'mess_meal_session_save.php',
  messMealTokenToday: 'mess_meal_token_today.php',
  messShopSettingGet: 'mess_shop_setting_get.php',
  messShopSettingSave: 'mess_shop_setting_save.php',

  getSupportTickets: 'getSupportTickets.php',
  createSupportTicket: 'createSupportTicket.php',
  getSupportTicketDetails: 'getSupportTicketDetails.php',
  replySupportTicket: 'replySupportTicket.php',

  getStaffList: 'getStaffList.php',
  getStaff: 'getStaff.php',
  insertStaff: 'insertStaff.php',
  updateStaff: 'updateStaff.php',
  deactivateStaff: 'deactivateStaff.php',
  resetStaffPin: 'resetStaffPin.php',
  getRoleDefaults: 'getRoleDefaults.php',
  getEffectivePermissions: 'getEffectivePermissions.php',
  getSalaryList: 'getSalaryList.php',
  saveSalaryPayment: 'saveSalaryPayment.php',

  getPosDeviceList: 'getPosDeviceList.php',
  registerPosDevice: 'registerPosDevice.php',
  revokePosDevice: 'revokePosDevice.php',

  getStorePrinterList: 'getStorePrinterList.php',
  insertStorePrinter: 'insertStorePrinter.php',
  createPrintJob: 'createPrintJob.php',
  getPrintJobList: 'getPrintJobList.php',
  acknowledgePrintJob: 'acknowledgePrintJob.php',
  retryPrintJob: 'retryPrintJob.php',
} as const

export const ResponseKeys = {
  foodTypeResponse: 'foodTypeResponse',
  categoryResponse: 'categoryResponse',
  subcategoryResponse: 'subcategoryResponse',
  productResponse: 'productResponse',
  portionResponse: 'portionResponse',
  portionMasterResponse: 'portionMasterResponse',
  comboResponse: 'comboResponse',
  comboItemResponse: 'comboItemResponse',
  posTableResponse: 'posTableResponse',
  diningAreaResponse: 'diningAreaResponse',
  tableTypeResponse: 'tableTypeResponse',
  diningSessionResponse: 'diningSessionResponse',
  memberResponse: 'memberResponse',
  messTokenResponse: 'messTokenResponse',
  messPaymentResponse: 'messPaymentResponse',
  messInvoiceResponse: 'messInvoiceResponse',
  companyResponse: 'companyResponse',
  invoiceResponse: 'invoiceResponse',
  invoiceProductResponse: 'invoiceProductResponse',
  invoiceComboItemResponse: 'invoiceComboItemResponse',
  inventoryResponse: 'inventoryResponse',
  expensesResponse: 'expensesResponse',
  tickets: 'tickets',
} as const
