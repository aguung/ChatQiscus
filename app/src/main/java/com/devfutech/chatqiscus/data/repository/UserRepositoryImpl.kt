package com.devfutech.chatqiscus.data.repository

import com.devfutech.chatqiscus.data.local.PreferenceProvider
import com.devfutech.chatqiscus.utils.Action
import com.devfutech.chatqiscus.utils.ObjectUtil
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import com.qiscus.sdk.chat.core.data.remote.QiscusApi
import rx.Emitter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(private val prefs: PreferenceProvider) :UserRepository{
    override fun login(
        email: String?,
        password: String?,
        name: String?,
        onSuccess: Action<QiscusAccount?>,
        onError: Action<Throwable?>
    ) {
        QiscusCore.setUser(email, password)
            .withUsername(name)
            .withAvatarUrl(ObjectUtil.generateAvatar(name))
            .save()
            .doOnNext(this::setCurrentUser)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun getCurrentUser(onSuccess: Action<QiscusAccount?>, onError: Action<Throwable?>) {
        getCurrentUserObservable().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun getUsers(
        page: Long,
        limit: Int,
        query: String?,
        onSuccess: Action<List<QiscusAccount?>?>,
        onError: Action<Throwable?>
    ) {
        QiscusApi.getInstance().getUsers(query, page, limit.toLong())
            .flatMap { iterable: List<QiscusAccount?>? ->
                Observable.from(
                    iterable
                )
            }
            .filter { user -> user != prefs.getCurrentUserLogin() }
            .filter { user -> user?.username != "" }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun updateProfile(
        name: String?,
        onSuccess: Action<QiscusAccount?>,
        onError: Action<Throwable?>
    ) {
        QiscusCore.updateUserAsObservable(name, prefs.getCurrentUserLogin().avatar)
            .doOnNext(this::setCurrentUser)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun logout() {
        prefs.logout()
    }

    override fun setDeviceToken(token: String?) {
        prefs.setCurrentDeviceToken(token!!)
    }

    private fun getCurrentUserObservable(): Observable<QiscusAccount?> {
        return Observable.create({ subscriber: Emitter<QiscusAccount?> ->
            try {
                subscriber.onNext(prefs.getCurrentUserLogin())
            } catch (e: Exception) {
                subscriber.onError(e)
            } finally {
                subscriber.onCompleted()
            }
        }, Emitter.BackpressureMode.BUFFER)
    }

    private fun setCurrentUser(qiscusAccount: QiscusAccount) {
        prefs.setUser(qiscusAccount)
    }
}