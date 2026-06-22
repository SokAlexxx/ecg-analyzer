package com.ecganalyzer.service;

import com.ecganalyzer.model.ECGAnnotation;
import java.util.Collections;
import java.util.List;

public class AnnotationOverlayService {

    public List<ECGAnnotation> findVisibleAnnotations(List<ECGAnnotation> annotations,
                                                      int startIndex,
                                                      int endIndex) {
        if (annotations == null || annotations.isEmpty()) {
            return Collections.emptyList();
        }

        return annotations.stream()
                .filter(annotation -> annotation.getSampleIndex() >= startIndex)
                .filter(annotation -> annotation.getSampleIndex() <= endIndex)
                .toList();
    }
}
