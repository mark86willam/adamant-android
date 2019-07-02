package im.adamant.android.ui.entities;

import android.content.Context;

import java.math.BigDecimal;

import im.adamant.android.R;

public abstract class TransferDetails {
    protected String id;
    protected BigDecimal amount;
    protected BigDecimal fee;
    protected long unixTransferDate;
    protected String fromId, toId;
    protected long confirmations;

    public static enum STATUS {
        PENDING, SUCCESS;

        public String getHumanString(Context ctx){
            if(this == TransferDetails.STATUS.SUCCESS){
                return ctx.getString(R.string.transaction_status_success);
            }else if(this== TransferDetails.STATUS.PENDING){
                return ctx.getString(R.string.transaction_status_pending);
            }else {
                throw new IllegalArgumentException("Can't find resource for this status");
            }
        }
    }

    public abstract STATUS getStatus();

    public String getId() {
        return id;
    }

    public TransferDetails setId(String id) {
        this.id = id;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferDetails setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public TransferDetails setFee(BigDecimal fee) {
        this.fee = fee;
        return this;
    }

    public long getUnixTransferDate() {
        return unixTransferDate;
    }

    public TransferDetails setUnixTransferDate(long unixTransferDate) {
        this.unixTransferDate = unixTransferDate;
        return this;
    }

    public String getFromId() {
        return fromId;
    }

    public TransferDetails setFromId(String fromId) {
        this.fromId = fromId;
        return this;
    }

    public String getToId() {
        return toId;
    }

    public TransferDetails setToId(String toId) {
        this.toId = toId;
        return this;
    }

    public long getConfirmations() {
        return confirmations;
    }

    public TransferDetails setConfirmations(long confirmations) {
        this.confirmations = confirmations;
        return this;
    }
}