package org.bobj.point.domain;

/**
 * 포인트 트랜잭션 유형을 정의하는 enum
 * 각 항목은 잔액 계산 시 더할지 뺄지를 isPositive로 판단함
 */
public enum PointTransactionType {

    DEPOSIT(true),     // 💰 사용자가 직접 충전한 포인트 → 잔액에 더함
    INVEST(false),     // 🏗️ 펀딩 등에 투자로 사용된 포인트 → 잔액에서 차감
    TRADE_SALE(true),  // 🏷️ 지분 매각으로 얻은 수익 → 잔액에 더함
    PAYOUT(true),      // 💵 프로젝트 수익 정산 → 잔액에 더함
    ALLOCATION(true),  // 🎁 배당금 지급 → 잔액에 더함
    WITHDRAW(false),   // 🔻 실제 출금된 환급 포인트 → 잔액에서 차감
    REFUND(true),      // 🔁 투자 실패/환급 실패 등으로 복원된 포인트 → 잔액에 더함
    CANCEL(true);      // 🚫 주문 취소로 되돌아온 포인트 → 잔액에 더함

    private final boolean isPositive;

    PointTransactionType(boolean isPositive) {
        this.isPositive = isPositive;
    }

    /**
     * 포인트 금액에 부호를 적용하는 유틸 메서드
     * 양수 유형이면 그대로, 음수 유형이면 음수로 반환
     */
    public long applySign(long amount) {
        return isPositive ? amount : -amount;
    }
}
