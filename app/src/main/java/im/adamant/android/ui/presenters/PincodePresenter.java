package im.adamant.android.ui.presenters;

import com.arellomobile.mvp.InjectViewState;

import java.io.IOException;

import im.adamant.android.BuildConfig;
import im.adamant.android.R;
import im.adamant.android.helpers.LoggerHelper;
import im.adamant.android.interactors.LogoutInteractor;
import im.adamant.android.interactors.SecurityInteractor;
import im.adamant.android.ui.mvp_view.PinCodeView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

@InjectViewState
public class PincodePresenter extends BasePresenter<PinCodeView> {
    private SecurityInteractor securityInteractor;
    private LogoutInteractor logoutInteractor;
    private PinCodeView.MODE mode = PinCodeView.MODE.ACCESS_TO_APP;
    private String pincodeForConfirmation;
    private int attemptsCount = 0;
    private Disposable currentOperation;

    public PincodePresenter(
            SecurityInteractor securityInteractor,
            LogoutInteractor logoutInteractor
    ) {
        this.securityInteractor = securityInteractor;
        this.logoutInteractor = logoutInteractor;
    }

    public void setMode(PinCodeView.MODE mode) {
        this.mode = mode;
        switch (mode) {
            case CREATE: {
                getViewState().setSuggestion(R.string.activity_pincode_enter_new_pincode);
                getViewState().setCancelButtonText(R.string.cancel);
            }
            break;
            case ACCESS_TO_APP: {
                getViewState().setSuggestion(R.string.activity_pincode_enter_pincode);
                getViewState().setCancelButtonText(R.string.activity_pincode_remove_pin);
            }
            break;
        }
    }

    public void onInputPincodeWasCompleted(CharSequence pinCode) {
        String pinCodeString = pinCode.toString();
        if (mode == PinCodeView.MODE.CREATE) {
            if (!validate(pinCodeString)) {
                return;
            }
        }

        if (currentOperation != null) {
            currentOperation.dispose();
        }

        switch (mode) {
            case CREATE: {
                mode = PinCodeView.MODE.CONFIRM;
                pincodeForConfirmation = pinCodeString;
                getViewState().setSuggestion(R.string.activity_pincode_confirm);
            }
            break;
            case CONFIRM: {
                if (pinCodeString.equals(pincodeForConfirmation)) {
                    getViewState().startProcess();
                    currentOperation = securityInteractor
                            .savePassphrase(pinCodeString)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        getViewState().stopProcess(true);
                                        getViewState().close();
                                    },
                                    error -> {
                                        getViewState().stopProcess(false);
                                        getViewState().showError(R.string.encryption_error);
                                        LoggerHelper.e("PINCODE", error.getMessage(), error);
                                    }
                            );
                } else {
                    getViewState().showError(R.string.pincode_unconfirmed);
                    getViewState().setSuggestion(R.string.activity_pincode_enter_new_pincode);
                    mode = PinCodeView.MODE.CREATE;
                }

                pincodeForConfirmation = null;

            }
            break;
            case ACCESS_TO_APP: {
                attemptsCount++;

                getViewState().startProcess();
                currentOperation = securityInteractor
                        .restoreAuthorizationByPincode(pinCodeString)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                (authorization) -> {
                                    getViewState().stopProcess(true);
                                    if (authorization.isSuccess()) {
                                        getViewState().goToMain();
                                    } else {
                                        getViewState().showError(R.string.account_not_found);
                                    }
                                },
                                error -> {
                                    getViewState().stopProcess(false);
                                    LoggerHelper.e("PINCODE", error.getMessage(), error);

                                    if (error instanceof IOException) {
                                        getViewState().showError(R.string.pincode_authorization_error);
                                        return;
                                    }

                                    if (attemptsCount == BuildConfig.MAX_WRONG_PINCODE_ATTEMTS) {
                                        //TODO: Do this in rx-stream
                                        logoutInteractor.logout();
                                        getViewState().stopProcess(true);
                                        getViewState().showError(R.string.pincode_exceeded_limit);
                                        getViewState().goToSplash();
                                    } else {
                                        getViewState().showWrongPin(BuildConfig.MAX_WRONG_PINCODE_ATTEMTS - attemptsCount);
                                    }
                                }
                        );
            }
            break;
        }
    }

    public void onClickCancelButton() {
        switch (mode) {
            case CREATE:
            case CONFIRM:
                getViewState().close();
                break;
            case ACCESS_TO_APP:
                getViewState().startProcess();
                Disposable pincodeReset = logoutInteractor
                        .getEventBus()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                (irrelevant) -> {
                                    getViewState().stopProcess(true);
                                    getViewState().goToSplash();
                                },
                                (error) -> {
                                    getViewState().showError(R.string.unsubscribe_push_error);
                                    getViewState().stopProcess(false);
                                    LoggerHelper.e("PINCODE", error.getMessage(), error);
                                }
                        );

                subscriptions.add(pincodeReset);

                logoutInteractor.logout();
        }
    }

    private boolean validate(CharSequence pincode) {

        //---
        boolean isSame = true;
        char previousChar = pincode.charAt(0);
        for (int i = 1; i < pincode.length(); i++) {
            if (previousChar != pincode.charAt(i)) {
                isSame = false;
                break;
            }
        }

        if (isSame) {
            getViewState().showError(R.string.wrong_pincode_one_symbol);
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        if (currentOperation != null) {
            currentOperation.dispose();
            currentOperation = null;
        }
        super.onDestroy();
    }

}
