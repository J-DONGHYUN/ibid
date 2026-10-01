package project.kjhjdh.ibid.trade.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.trade.application.CompletionService;
import project.kjhjdh.ibid.trade.presentation.dto.CompleteRequest;

@RestController
@RequestMapping("/api/products/{productId}/completion")
@RequiredArgsConstructor
public class CompletionController {

    private final CompletionService completionService;

    @PostMapping
    public ResponseEntity<Void> complete(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId,
            @Valid @RequestBody CompleteRequest request
    ) {
        completionService.complete(productId, loginUser.userId(), request.buyerId());
        return ResponseEntity.ok().build();
    }
}
