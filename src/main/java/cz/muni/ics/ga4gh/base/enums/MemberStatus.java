package cz.muni.ics.ga4gh.base.enums;

public enum MemberStatus {

    VALID,
    EXPIRED,
    INVALID,
    DISABLED,
    SUSPENDED;

    public static MemberStatus fromString(String status) {
        if (status == null) {
            return null;
        }

        switch (status.toUpperCase().trim()) {
            case "VALID":
                return VALID;
            case "INVALID":
                return INVALID;
            case "EXPIRED":
                return EXPIRED;
            case "DISABLED":
                return DISABLED;
            case "SUSPENDED":
                return SUSPENDED;
            default:
                return null;
        }
    }
}
