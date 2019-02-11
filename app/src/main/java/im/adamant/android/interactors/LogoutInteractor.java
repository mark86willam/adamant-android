package im.adamant.android.interactors;

import java.util.concurrent.TimeUnit;

import im.adamant.android.core.AdamantApiWrapper;
import im.adamant.android.helpers.ChatsStorage;
import im.adamant.android.helpers.Settings;
import im.adamant.android.rx.Irrelevant;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class LogoutInteractor {
    private ChatsStorage chatsStorage;
    private Settings settings;
    private AdamantApiWrapper api;
    private SubscribeToPushInteractor subscribeToPushInteractor;
    private RefreshChatsInteractor refreshChatsInteractor;

    private PublishSubject<Irrelevant> publisher = PublishSubject.create();
    private Flowable eventBus = publisher.toFlowable(BackpressureStrategy.LATEST);
    private Disposable logoutDisposable;

    public LogoutInteractor(
            ChatsStorage chatsStorage,
            Settings settings,
            AdamantApiWrapper api,
            SubscribeToPushInteractor subscribeToPushInteractor,
            RefreshChatsInteractor refreshChatsInteractor
    ) {
        this.chatsStorage = chatsStorage;
        this.settings = settings;
        this.api = api;
        this.subscribeToPushInteractor = subscribeToPushInteractor;
        this.refreshChatsInteractor = refreshChatsInteractor;
    }

    public Flowable<Irrelevant> getEventBus() {
        return eventBus;
    }

    public void execute() {
        if (logoutDisposable != null) {
            logoutDisposable.dispose();
        }

        logoutDisposable = subscribeToPushInteractor
                .getEventsObservable()
                .timeout(30, TimeUnit.SECONDS)
                .subscribe(
                        (event) -> {
                            if ((event == SubscribeToPushInteractor.Event.UNSUBSCRIBED) || (event == SubscribeToPushInteractor.Event.IGNORED)) {
                                refreshChatsInteractor.cleanUp();
                                chatsStorage.cleanUp();
                                api.logout();
                                settings.setAccountKeypair("");
                                settings.setKeyPairMustBeStored(false);

                                publisher.onNext(Irrelevant.INSTANCE);
                                logoutDisposable.dispose();
                                logoutDisposable = null;
                            }
                        },
                        (error) -> {
                            publisher.onError(error);
                            logoutDisposable.dispose();
                            logoutDisposable = null;
                        }
                );

        subscribeToPushInteractor.deleteCurrentToken();
    }

    @Override
    protected void finalize() throws Throwable {

        if (logoutDisposable != null){
            logoutDisposable.dispose();
        }
        super.finalize();
    }
}
