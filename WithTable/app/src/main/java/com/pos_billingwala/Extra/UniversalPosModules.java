package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Extra.dynamic.ModuleRegistry;
import com.pos_billingwala.Extra.dynamic.POSConfiguration;
import com.pos_billingwala.Extra.dynamic.PosConfigCache;

/**
 * Registry of Universal POS module facades (phases 01–22 + Dynamic pack).
 * Useful for Settings diagnostics and Cursor implementation prompts.
 */
public final class UniversalPosModules {

    private UniversalPosModules() {
    }

    public static String fullDiagnostics(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Universal POS diagnostics ===\n");
        sb.append(POSConfiguration.resolve(context).summaryLine()).append('\n');
        sb.append(PosConfigCache.summaryLine(context)).append('\n');
        sb.append(ModuleRegistry.summaryLine(context)).append('\n');
        sb.append(BusinessTemplateEngine.summaryLine(context)).append('\n');
        sb.append(RestaurantFoodModule.moduleSummary(context)).append('\n');
        sb.append(BarRestaurantModule.moduleSummary(context)).append('\n');
        sb.append(MessModule.moduleSummary(context)).append('\n');
        sb.append(WeightFreshModule.moduleSummary(context)).append('\n');
        sb.append(RetailGroceryModule.moduleSummary(context)).append('\n');
        sb.append(FashionJewelleryModule.moduleSummary(context)).append('\n');
        sb.append(SalonAppointmentModule.moduleSummary(context)).append('\n');
        sb.append(CakeBakeryModule.moduleSummary(context)).append('\n');
        sb.append(ElectronicsModule.moduleSummary(context)).append('\n');
        sb.append(RepairModule.moduleSummary(context)).append('\n');
        sb.append(RentalModule.moduleSummary(context)).append('\n');
        sb.append(ProductServiceEngine.moduleSummary(context)).append('\n');
        sb.append(ImportExportEngine.moduleSummary(context)).append('\n');
        sb.append(InventoryStockEngine.moduleSummary(context)).append('\n');
        sb.append(UniversalPrinterEngine.moduleSummary(context)).append('\n');
        sb.append(AdminDealerPlatform.moduleSummary()).append('\n');
        sb.append(CrashApiDeviceLogging.moduleSummary(context)).append('\n');
        sb.append(DynamicSettingsReports.moduleSummary(context)).append('\n');
        sb.append(DynamicUiEngine.moduleSummary(context)).append('\n');
        sb.append(DatabaseApiArchitecture.moduleSummary()).append('\n');
        sb.append(OfflineSyncArchitecture.moduleSummary()).append('\n');
        sb.append(SecurityPermissions.moduleSummary(context));
        return sb.toString();
    }
}
