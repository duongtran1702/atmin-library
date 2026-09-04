package atmin.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void keepsExistingSuccessFactoriesCompatible() {
        ApiResponse<String> response = ApiResponse.success("Done", "payload");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Done");
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getPage()).isNull();
    }

    @Test
    void supportsCreatedAcceptedAndCustomStatuses() {
        assertThat(ApiResponse.created("Created", 1).getStatus()).isEqualTo(201);
        assertThat(ApiResponse.accepted("Queued", 1).getStatus()).isEqualTo(202);
        assertThat(ApiResponse.success(HttpStatus.PARTIAL_CONTENT, "Partial", 1).getStatus())
                .isEqualTo(206);
    }

    @Test
    void createsResponseDirectlyFromPage() {
        PageImpl<String> page = new PageImpl<>(
                List.of("a", "b"), PageRequest.of(1, 2), 5);

        ApiResponse<List<String>> response = ApiResponse.pagePaginated("Items", page);

        assertThat(response.getData()).containsExactly("a", "b");
        assertThat(response.getPage().getPageNumber()).isEqualTo(1);
        assertThat(response.getPage().getTotalElements()).isEqualTo(5);
        assertThat(response.getPage().isHasNext()).isTrue();
    }

    @Test
    void keepsExistingSliceFactoryCompatible() {
        SliceImpl<String> slice = new SliceImpl<>(
                List.of("a", "b"), PageRequest.of(0, 2), true);

        ApiResponse<List<String>> response = ApiResponse.slicePaginated("Items", slice);

        assertThat(response.getData()).containsExactly("a", "b");
        assertThat(response.getPage().getTotalElements()).isNull();
        assertThat(response.getPage().isHasNext()).isTrue();
    }
}
