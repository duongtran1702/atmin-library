package atmin.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PageInfoTest {

    @Test
    void calculatesManualPaginationWithoutFloatingPointRounding() {
        PageInfo page = PageInfo.of(1, 10, 25);

        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.getFirst()).isFalse();
        assertThat(page.getLast()).isFalse();
    }

    @Test
    void supportsManualNoCountPagination() {
        PageInfo page = PageInfo.slice(3, 20, true);

        assertThat(page.getPageNumber()).isEqualTo(3);
        assertThat(page.getPageSize()).isEqualTo(20);
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.getTotalElements()).isNull();
        assertThat(page.getTotalPages()).isNull();
    }

    @Test
    void rejectsInvalidManualPagination() {
        assertThatIllegalArgumentException().isThrownBy(() -> PageInfo.of(-1, 10, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> PageInfo.of(0, 0, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> PageInfo.of(0, 10, -1));
        assertThatIllegalArgumentException().isThrownBy(() -> PageInfo.slice(0, 0, false));
    }
}
