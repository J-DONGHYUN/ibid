package project.kjhjdh.ibid.trade.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.trade.application.ReservationService;
import project.kjhjdh.ibid.trade.presentation.dto.ReserveRequest;

@RestController
@RequestMapping("/api/products/{productId}/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Void> reserve(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId,
            @Valid @RequestBody ReserveRequest request
    ) {
        reservationService.reserve(productId, loginUser.userId(), request.buyerId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelReservation(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId
    ) {
        reservationService.cancelReservation(productId, loginUser.userId());
        return ResponseEntity.ok().build();
    }
}
