/**
 * Offline / local preview data when admin API is unreachable.
 * Matches p23_website_catalog.sql seed structure.
 */
(function () {
  window.PBW_LOCAL_MOCK = {
    settings: {
      legal_company_name: "CANA Tech Solutions Private Limited",
      brand_tagline: "Smart Billing. Trusted Support. Better Business.",
      gstin: "27XXXXX1234X1ZX",
      office_address:
        "S No 47, Pune - Satara Rd, opp. City Pride Multiplex, near Bhapkar petrol pump, Adinath Society, Taware Colony, Bibwewadi, Pune, Maharashtra 411009",
      support_phone: "8983149299",
      support_whatsapp: "8983149299",
      support_email: "support@posbillingwala.com",
      business_hours: "Mon–Sat, 10:00 AM – 7:00 PM IST",
      play_store_url:
        "https://play.google.com/store/apps/details?id=com.pos_billingwala",
      apk_download_url: "",
      app_latest_version: "Latest",
    },
    products: [
      {
        name: "POS Billing Software",
        category: "software",
        description:
          "Android mobile & tablet billing with offline sync, licensing, and thermal print.",
        icon: "📱",
      },
      {
        name: "Android Mobile & Tablet Billing",
        category: "software",
        description:
          "Counter-ready POS app for restaurants, retail, mess, and takeaway.",
        icon: "📲",
      },
      {
        name: "POS Machine",
        category: "hardware",
        description:
          "Counter POS hardware compatible with POS Billingwala workflows.",
        icon: "🖥️",
      },
      {
        name: "Bluetooth / Thermal Printer",
        category: "hardware",
        description:
          "57mm and 80mm Bluetooth thermal printers for fast receipt printing.",
        icon: "🖨️",
      },
      {
        name: "57mm Billing Rolls",
        category: "consumables",
        description: "Thermal billing rolls for compact receipt printers.",
        icon: "🧾",
      },
      {
        name: "80mm Billing Rolls",
        category: "consumables",
        description:
          "Standard-width thermal rolls for restaurant and retail counters.",
        icon: "🧾",
      },
      {
        name: "Barcode Labels & Ribbons",
        category: "consumables",
        description: "Labels and ribbons for inventory and retail tagging.",
        icon: "🏷️",
      },
      {
        name: "Accessories",
        category: "accessories",
        description: "Cables, stands, and billing counter accessories.",
        icon: "🔌",
      },
    ],
    plans: [
      {
        plan_type: "subscription",
        validity_label: "6 Months",
        price: 3500,
        gst_note: "GST included",
        description:
          "Standard 6-month plan\n6 months validity\nOffline billing mode\nCustomer management\nPriority phone support\nGST billing & invoices\nSingle device licence\nProduct & stock management\nBluetooth thermal printing\nSales reports\nExtend existing licence\nKeep all your data\nGST billing & reports\nBluetooth printing\nPhone support",
      },
      {
        plan_type: "subscription",
        validity_label: "1 Year",
        price: 6000,
        gst_note: "GST included",
        is_featured: 1,
        description:
          "Everything in 1-year plan\n1 year validity — best value\nOffline billing mode\nCustomer management\nPriority phone support\nGST billing & invoices\nSingle device licence\nProduct & stock management\nBluetooth thermal printing\nSales reports\nExtend existing licence\nKeep all your data\nGST billing & reports\nBluetooth printing\nPhone support",
      },
      {
        plan_type: "renewal",
        validity_label: "6 Months",
        price: 3000,
        gst_note: "GST included",
        description:
          "Standard 6-month plan\n6 months validity\nOffline billing mode\nCustomer management\nPriority phone support\nGST billing & invoices\nSingle device licence\nProduct & stock management\nBluetooth thermal printing\nSales reports\nExtend existing licence\nKeep all your data\nGST billing & reports\nBluetooth printing\nPhone support",
      },
      {
        plan_type: "renewal",
        validity_label: "1 Year",
        price: 5500,
        gst_note: "GST included",
        description:
          "Everything in 1-year plan\n1 year validity — best value\nOffline billing mode\nCustomer management\nPriority phone support\nGST billing & invoices\nSingle device licence\nProduct & stock management\nBluetooth thermal printing\nSales reports\nExtend existing licence\nKeep all your data\nGST billing & reports\nBluetooth printing\nPhone support",
      },
    ],
    dealers: [
      {
        area: "Pune",
        dealer_name: "Pune Office",
        contact_person: "Santosh Dixit",
        role_title: "Sales & Marketing Manager",
        mobile: "8983149299",
        whatsapp: "8983149299",
        address:
          "S No 47, Pune - Satara Rd, opp. City Pride Multiplex, near Bhapkar petrol pump, Adinath Society, Taware Colony, Bibwewadi, Pune, Maharashtra 411009",
        map_url: "",
        dealer_type: "head_office",
      },
      {
        area: "Pandharpur",
        dealer_name: "Authorized POS Billingwala Dealer",
        contact_person: "",
        role_title: "Authorized Dealer",
        mobile: "",
        whatsapp: "",
        address: "Pandharpur, Maharashtra, India",
        map_url: "",
        dealer_type: "authorized_dealer",
      },
      {
        area: "Satara",
        dealer_name: "Authorized POS Billingwala Dealer — Satara",
        contact_person: "",
        role_title: "Authorized Dealer",
        mobile: "",
        whatsapp: "",
        address: "Satara, Maharashtra, India",
        map_url: "",
        dealer_type: "authorized_dealer",
      },
      {
        area: "Solapur",
        dealer_name: "Authorized POS Billingwala Dealer — Solapur",
        contact_person: "",
        role_title: "Authorized Dealer",
        mobile: "",
        whatsapp: "",
        address: "Solapur, Maharashtra, India",
        map_url: "",
        dealer_type: "authorized_dealer",
      },
      {
        area: "Mumbai",
        dealer_name: "Authorized POS Billingwala Dealer — Mumbai",
        contact_person: "",
        role_title: "Authorized Dealer",
        mobile: "",
        whatsapp: "",
        address: "Mumbai, Maharashtra, India",
        map_url: "",
        dealer_type: "authorized_dealer",
      },
      {
        area: "Karnataka",
        dealer_name: "Authorized POS Billingwala Dealer — Karnataka",
        contact_person: "",
        role_title: "Regional Dealer",
        mobile: "",
        whatsapp: "",
        address: "Karnataka, India",
        map_url: "",
        dealer_type: "authorized_dealer",
      },
    ],
    clients: [
      {
        business_name: "Hotel Shree",
        subtitle: "Owner · Pune",
        city: "Pune",
        business_category: "Restaurant",
        description:
          "Daily billing, KOT printing, and table-wise sales with POS Billingwala.",
        logo_url: "",
        photo_url: "",
        cta_url: "",
      },
      {
        business_name: "Balaji Kirana Store",
        subtitle: "Retail · Pandharpur",
        city: "Pandharpur",
        business_category: "Retail",
        description:
          "Fast barcode billing and thermal receipt printing for daily customers.",
        logo_url: "",
        photo_url: "",
        cta_url: "",
      },
      {
        business_name: "Mess Prasad",
        subtitle: "Mess · Solapur",
        city: "Solapur",
        business_category: "Mess",
        description: "Mess token billing and member payment tracking.",
        logo_url: "",
        photo_url: "",
        cta_url: "",
      },
    ],
    testimonials: [
      {
        author_name: "Rajesh Patil",
        business_name: "Hotel Shree, Pune",
        quote:
          "Offline billing never stops even when internet goes down. Local dealer support is excellent.",
        rating: 5,
        photo_url: "",
      },
    ],
  };
})();
