package im.adamant.android.interactors;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import im.adamant.android.core.AdamantApiWrapper;
import im.adamant.android.helpers.PublicKeyStorage;
import im.adamant.android.interactors.chats.ChatsStorage;
import im.adamant.android.interactors.wallets.SupportedWalletFacadeType;
import im.adamant.android.interactors.wallets.WalletFacade;
import im.adamant.android.interactors.wallets.entities.TransferDetails;
import im.adamant.android.ui.entities.Chat;
import im.adamant.android.ui.entities.UITransferDetails;
import io.reactivex.Flowable;

public class TransferDetailsInteractor {
    private Map<SupportedWalletFacadeType, WalletFacade> wallets;
    private ChatsStorage chatsStorage;
    private AdamantApiWrapper api;
    private PublicKeyStorage publicKeyStorage;


    public TransferDetailsInteractor(AdamantApiWrapper api, Map<SupportedWalletFacadeType, WalletFacade> wallets, ChatsStorage chatsStorage,
                                     PublicKeyStorage keyStorage) {
        this.wallets = wallets;
        this.chatsStorage = chatsStorage;
        this.api = api;
        this.publicKeyStorage = keyStorage;
    }

    private final DecimalFormat decimalFormatter = new DecimalFormat("#.###");
    private final DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.getDefault());

    @Nullable
    private String getAddressName(String id) {
        Chat chat = chatsStorage.findChatByCompanionId(id);
        if (chat == null) {
            return null;
        }
        String title = chat.getTitle();
        if (id.equals(title)) {
            return null;
        } else {
            return title;
        }
    }

    private String accountId;
    private WalletFacade walletFacade;
    private String abbr;

    public Flowable<UITransferDetails> getTransferDetails(String id, String abbr) {
        this.abbr = abbr;
        SupportedWalletFacadeType supportedCurrencyType = SupportedWalletFacadeType.valueOf(abbr);
        walletFacade = wallets.get(supportedCurrencyType);

        if (walletFacade == null) { return Flowable.empty(); }
        return walletFacade.getTransferDetails(id)
                .map(this::getUiTransferDetails);
    }

    private String formatValue(BigDecimal value) {
        return String.format(Locale.getDefault(), "%s %s",
                decimalFormatter.format(value), abbr);
    }

    @NotNull
    private UITransferDetails getUiTransferDetails(TransferDetails details) {
        accountId = api.getAccount().getAddress();

        UITransferDetails result = new UITransferDetails()
                .setId(details.getId())
                .setAmount(formatValue(details.getAmount()))
                .setConfirmations(details.getConfirmations())
                .setFee(formatValue(details.getFee()))
                .setFromId(details.getFromId())
                .setFromAddress(getAddressName(details.getFromId()))
                .setToId(details.getToId())
                .setToAddress(getAddressName(details.getToId()))
                .setDate(dateFormat.format(new Date(details.getUnixTransferDate())))
                .setExplorerLink(walletFacade.getExplorerUrl(details.getId()))
                .setStatus(details.getStatus());

        if (accountId.equals(details.getFromId())) {
            result.setDirection(UITransferDetails.Direction.SENT);
        } else if (accountId.equals(details.getToId())) {
            result.setDirection(UITransferDetails.Direction.RECEIVED);
        } else {
            throw new IllegalArgumentException("User is not related to this " +
                    "transaction");
        }

        if (result.getDirection() == UITransferDetails.Direction.SENT) {
            result.setHaveChat(chatsStorage.
                    findChatByCompanionId(details.getToId()) != null);
            publicKeyStorage.setPublicKey(details.getToId(), details.getReceiverPublicKey());
        } else if (result.getDirection() == UITransferDetails.Direction.RECEIVED) {
            result.setHaveChat(chatsStorage.
                    findChatByCompanionId(details.getFromId()) != null);
            publicKeyStorage.setPublicKey(details.getFromId(), details.getSenderPublicKey());
        } else {
            throw new IllegalArgumentException("Unknown direction");
        }

        return result;
    }
}
