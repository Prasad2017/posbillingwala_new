package com.pos_billingwala.Extra;

/**
 * Kitchen vs bar ticket routing for Bar + Restaurant.
 * When BOT is on, {@link com.pos_billingwala.Activity.BluetoothPrint} prints KOT then BOT batches.
 */
public enum TicketRoute {
    KITCHEN_KOT("KOT", "Kitchen"),
    BAR_BOT("BOT", "Bar");

    private final String ticketCode;
    private final String stationLabel;

    TicketRoute(String ticketCode, String stationLabel) {
        this.ticketCode = ticketCode;
        this.stationLabel = stationLabel;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getStationLabel() {
        return stationLabel;
    }
}
