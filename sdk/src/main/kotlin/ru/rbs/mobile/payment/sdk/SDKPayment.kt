package ru.rbs.mobile.payment.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import ru.rbs.mobile.payment.sdk.Constants.REQUEST_CODE_CRYPTOGRAM
import ru.rbs.mobile.payment.sdk.component.CryptogramProcessor
import ru.rbs.mobile.payment.sdk.component.impl.DefaultCryptogramProcessor
import ru.rbs.mobile.payment.sdk.component.impl.DefaultPaymentStringProcessor
import ru.rbs.mobile.payment.sdk.component.impl.RSACryptogramCipher
import ru.rbs.mobile.payment.sdk.model.PaymentConfig
import ru.rbs.mobile.payment.sdk.model.PaymentData
import ru.rbs.mobile.payment.sdk.model.SDKConfig
import ru.rbs.mobile.payment.sdk.ui.CardListActivity
import ru.rbs.mobile.payment.sdk.ui.CardNewActivity
import ru.rbs.mobile.payment.sdk.ui.helper.LocalizationSetting

/**
 * Основной класс для работы с функционалом библиотеки оплаты из мобильного приложения.
 */
object SDKPayment {

    @JvmSynthetic
    internal var innerSdkConfig: SDKConfig? = null
    internal val sdkConfig: SDKConfig
        get() = innerSdkConfig
            ?: throw IllegalStateException("Please call SDKPayment.init() before.")

    @JvmSynthetic
    internal var innerCryptogramProcessor: CryptogramProcessor? = null
        get() {
            return field ?: DefaultCryptogramProcessor(
                keyProvider = sdkConfig.keyProvider,
                paymentStringProcessor = DefaultPaymentStringProcessor(),
                cryptogramCipher = RSACryptogramCipher()
            )
        }
        set(value) {
            field = value
        }
    internal val cryptogramProcessor: CryptogramProcessor
        get() {
            return innerCryptogramProcessor
                ?: throw IllegalStateException("Please call SDKPayment.init() before.")
        }

    /**
     * Инициализация.
     */
    fun init(context: Context) {
        innerSdkConfig = SDKConfigBuilder(context).build()
    }

    /**
     * Инициализация.
     */
    fun init(sdkConfig: SDKConfig) {
        innerSdkConfig = sdkConfig
    }

    /**
     * Запуск процесса оплаты.
     *
     * @param activity в которую будет возвращаться результат.
     * @param config конфигурация оплаты.
     */
    fun cryptogram(activity: Activity, config: PaymentConfig) {
        checkNotNull(cryptogramProcessor)
        LocalizationSetting.setLanguage(config.locale)
        if (config.cards.isEmpty()) {
            activity.startActivityForResult(
                CardNewActivity.prepareIntent(activity, config),
                REQUEST_CODE_CRYPTOGRAM
            )
        } else {
            activity.startActivityForResult(
                CardListActivity.prepareIntent(activity, config),
                REQUEST_CODE_CRYPTOGRAM
            )
        }
    }

    /**
     * Обработка результата оплаты.
     */
    fun handleResult(
        requestCode: Int,
        data: Intent?,
        callback: ResultCallback<PaymentData>
    ): Boolean = if (data != null && REQUEST_CODE_CRYPTOGRAM == requestCode) {
        handlePaymentResult(data, callback)
        true
    } else {
        false
    }

    private fun handlePaymentResult(data: Intent, callback: ResultCallback<PaymentData>) {
        val paymentData = data.getSerializableExtra(Constants.INTENT_EXTRA_RESULT) as PaymentData?
        if (paymentData != null) {
            callback.onSuccess(paymentData)
        } else {
            callback.onFail(IllegalStateException("Payment data is empty"))
        }
    }
}
