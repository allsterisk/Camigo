package com.camigo.common;

/**
 * Camigo 稳定业务错误码。模块按千位区分，避免接口返回数据库异常文本。
 */
public final class ErrorCodes {

    public static final int USER_CONFLICT = 41001;
    public static final int USER_INVALID_CREDENTIALS = 41002;
    public static final int USER_DISABLED = 41003;
    public static final int USER_NOT_FOUND = 41004;
    public static final int ROLE_NOT_CONFIGURED = 41005;
    public static final int USER_PROFILE_INVALID = 41006;
    public static final int USER_CANCELLED = 41007;

    public static final int PARCEL_NOT_FOUND = 42001;
    public static final int PARCEL_CONFLICT = 42002;
    public static final int PARCEL_STATUS_INVALID = 42003;

    public static final int DELIVERY_NOT_FOUND = 43001;
    public static final int DELIVERY_CONFLICT = 43002;
    public static final int DELIVERY_STATUS_INVALID = 43003;
    public static final int COURIER_NOT_FOUND = 43004;
    public static final int DISPATCH_CONFLICT = 43005;
    public static final int DISPATCH_OFFER_EXPIRED = 43006;
    public static final int COURIER_LOCATION_REQUIRED = 43007;
    public static final int ROUTE_STEP_CONFLICT = 43008;

    public static final int LOCKER_NOT_FOUND = 44001;
    public static final int SLOT_NOT_AVAILABLE = 44002;
    public static final int STORAGE_NOT_FOUND = 44003;
    public static final int PICKUP_CODE_INVALID = 44004;
    public static final int PICKUP_CONFLICT = 44005;

    public static final int EVALUATION_CONFLICT = 45001;
    public static final int EVALUATION_NOT_ALLOWED = 45002;

    private ErrorCodes() {
    }
}
