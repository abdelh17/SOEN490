package com.quolance.quolance_api.util;

import com.quolance.quolance_api.dtos.PageableRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class PaginationUtils {

    public Pageable createPageable(PageableRequestDto request) {
        return PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSort()
        );
    }
}
