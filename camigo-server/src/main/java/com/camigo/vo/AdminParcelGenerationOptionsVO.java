package com.camigo.vo;

import java.util.List;

public record AdminParcelGenerationOptionsVO(
        List<AdminStudentOptionVO> students,
        List<CampusLocationOptionVO> pickupLocations,
        List<CampusLocationOptionVO> destinationLocations,
        List<String> sizeClasses,
        int maxBatchSize) {
}
