package project.kjhjdh.ibid.support.seed;

import java.time.LocalDateTime;
import java.util.Random;

import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;

public class SeedRandomData {

    private static final String[] MODELS = {
            "애플 아이폰 15 Pro", "애플 아이폰 14", "삼성 갤럭시 S24", "삼성 갤럭시 Z플립5",
            "애플 맥북 에어 M3", "애플 맥북 프로 16", "애플 아이패드 프로 11", "애플 에어팟 프로 2",
            "삼성 갤럭시 버즈3", "삼성 갤럭시 워치6", "소니 플레이스테이션 5", "닌텐도 스위치 OLED",
            "다이슨 V15 무선청소기", "보스 QC45 헤드폰", "LG 그램 17", "레노버 씽크패드 X1", "델 XPS 13"
    };
    private static final String[] SPECS = {
            "256GB", "512GB", "1TB", "8GB/256GB", "16GB/512GB", "블랙", "화이트", "실버", "네이비", "정품"
    };
    private static final String[] TAG_WORDS = {
            "애플", "삼성", "노트북", "태블릿", "이어폰", "헤드폰", "스마트워치", "게임기", "카메라", "모니터",
            "키보드", "마우스", "충전기", "케이스", "미개봉", "풀박스", "리퍼", "정품", "무선", "블루투스"
    };

    private static final int MIN_PRICE = 10_000;
    private static final int MAX_VIEW_EXPONENT = 5;
    private static final int SPREAD_DAYS = 730;
    private static final int[] SHIPPING_FEES = {2500, 3000, 3500, 4000};
    private static final OrderStatus[] ORDER_STATUSES = {
            OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.SHIPPED_TO_INSPECTOR,
            OrderStatus.UNDER_INSPECTION, OrderStatus.COMPLETED, OrderStatus.REFUNDED, OrderStatus.CANCELED
    };
    private static final double[] ORDER_STATUS_WEIGHTS = {0.20, 0.25, 0.10, 0.10, 0.30, 0.03, 0.02};

    private final Random random;
    private final int userCount;
    private final int tagCount;
    private final int productCount;
    private final LocalDateTime baseTime;

    private final int[] productSeller;
    private final int[] productPrice;

    public SeedRandomData(long seed, int userCount, int tagCount, int productCount, LocalDateTime baseTime) {
        this.random = new Random(seed);
        this.userCount = userCount;
        this.tagCount = tagCount;
        this.productCount = productCount;
        this.baseTime = baseTime;
        this.productSeller = new int[productCount];
        this.productPrice = new int[productCount];
    }

    public String userEmail(int index) {
        return "seed" + index + "@ibid.test";
    }

    public String username(int index) {
        return String.format("usr%05d", index % 100_000);
    }

    public String tagName(int index) {
        if (index < TAG_WORDS.length) {
            return TAG_WORDS[index];
        }
        return TAG_WORDS[index % TAG_WORDS.length] + (index / TAG_WORDS.length);
    }

    public long sellerIdFor(int productIndex) {
        long sellerId = 1 + (long) (userCount * Math.pow(random.nextDouble(), 3));
        productSeller[productIndex] = (int) sellerId;
        return sellerId;
    }

    public String title(int productIndex) {
        return MODELS[random.nextInt(MODELS.length)] + " "
                + SPECS[random.nextInt(SPECS.length)] + " #" + productIndex;
    }

    public String description(String title) {
        return title + " 판매합니다. 사용감 있으나 기능 이상 없습니다. 플랫폼 검수 후 배송됩니다.";
    }

    public int price(int productIndex) {
        int price = (int) (MIN_PRICE * Math.pow(10, random.nextDouble() * 2.7) / 1000) * 1000;
        int bounded = Math.max(MIN_PRICE, price);
        productPrice[productIndex] = bounded;
        return bounded;
    }

    public ProductStatus productStatus() {
        double roll = random.nextDouble();
        if (roll < 0.05) {
            return ProductStatus.PENDING;
        }
        if (roll < 0.20) {
            return ProductStatus.SOLD_OUT;
        }
        return ProductStatus.ON_SALE;
    }

    public int stock(ProductStatus status) {
        return status == ProductStatus.SOLD_OUT ? 0 : 1 + random.nextInt(10);
    }

    public long viewCount() {
        return (long) Math.pow(10, random.nextDouble() * MAX_VIEW_EXPONENT);
    }

    public ProductCondition productCondition() {
        double roll = random.nextDouble();
        if (roll < 0.10) {
            return ProductCondition.NEW;
        }
        if (roll < 0.45) {
            return ProductCondition.LIKE_NEW;
        }
        return ProductCondition.USED;
    }

    public int shippingFee() {
        if (random.nextDouble() < 0.30) {
            return 0;
        }
        return SHIPPING_FEES[random.nextInt(SHIPPING_FEES.length)];
    }

    public LocalDateTime createdAt() {
        int daysAgo = (int) (SPREAD_DAYS * Math.pow(random.nextDouble(), 2));
        return baseTime.minusDays(daysAgo).minusSeconds(random.nextInt(86_400));
    }

    public int popularProductIndex() {
        return (int) (productCount * Math.pow(random.nextDouble(), 3));
    }

    public int uniformProductIndex() {
        return random.nextInt(productCount);
    }

    public int imageSortOrder() {
        return random.nextInt(4);
    }

    public String imageUrl(int productId, int sortOrder) {
        return "https://ibid-product-images.s3.ap-northeast-2.amazonaws.com/products/"
                + productId + "/seed-" + sortOrder + ".jpg";
    }

    public int tagsPerProduct() {
        return random.nextInt(5);
    }

    public long tagIdFor(int productIndex, int slot) {
        return 1 + ((long) productIndex * 7 + slot * 13L) % tagCount;
    }

    public long sellerOf(int productIndex) {
        return productSeller[productIndex];
    }

    public int priceOf(int productIndex) {
        return productPrice[productIndex];
    }

    public int quantity() {
        return 1 + random.nextInt(2);
    }

    public long buyerIdOtherThan(long sellerId) {
        long buyerId = 1 + random.nextInt(userCount);
        return buyerId == sellerId ? (buyerId % userCount) + 1 : buyerId;
    }

    public OrderStatus orderStatus() {
        double roll = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < ORDER_STATUSES.length; i++) {
            cumulative += ORDER_STATUS_WEIGHTS[i];
            if (roll < cumulative) {
                return ORDER_STATUSES[i];
            }
        }
        return OrderStatus.COMPLETED;
    }
}
