package com.quolance.quolance_api.dtos;

import com.quolance.quolance_api.util.validators.ValidSortField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PageableRequestDto {
    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;


    @ValidSortField
    private String sortBy = "id";

    @Pattern(regexp = "^(asc|desc)$", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String sortDirection = "desc";

    public Sort getSort() {
        return Sort.by(
                Sort.Direction.fromString(sortDirection),
                sortBy.split(",")
        );
    }

    public PageRequest toPageRequest() {
        return PageRequest.of(page, size, getSort());
    }
}
