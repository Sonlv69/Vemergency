package com.kiluss.vemergency.ui.shop.main

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.kiluss.vemergency.constant.PENDING_TRANSACTION_COLLECTION
import com.kiluss.vemergency.constant.SHOP_COLLECTION
import com.kiluss.vemergency.constant.SHOP_PENDING_COLLECTION
import com.kiluss.vemergency.data.firebase.FirebaseManager
import com.kiluss.vemergency.data.model.LatLng
import com.kiluss.vemergency.data.model.Shop
import com.kiluss.vemergency.data.model.Transaction
import com.kiluss.vemergency.ui.base.BaseViewModel
import com.kiluss.vemergency.utils.Utils

/**
 * Created by sonlv on 10/17/2022
 */
class ShopMainViewModel(application: Application) : BaseViewModel(application) {
    private var myShop: Shop? = null
    val db = Firebase.firestore
    private var pendingTransactions = mutableListOf<Transaction>()
    private val _avatarBitmap: MutableLiveData<Bitmap> by lazy {
        MutableLiveData<Bitmap>()
    }
    internal val avatarBitmap: LiveData<Bitmap> = _avatarBitmap
    private val _progressBarStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    internal val progressBarStatus: LiveData<Boolean> = _progressBarStatus
    private val _shop: MutableLiveData<Shop> by lazy {
        MutableLiveData<Shop>()
    }
    internal val shop: LiveData<Shop> = _shop
    private val _navigateToHome: MutableLiveData<Any> by lazy {
        MutableLiveData<Any>()
    }
    internal val navigateToHome: LiveData<Any> = _navigateToHome
    private val _pendingTransaction: MutableLiveData<MutableList<Transaction>> by lazy {
        MutableLiveData<MutableList<Transaction>>()
    }
    internal val pendingTransaction: LiveData<MutableList<Transaction>> = _pendingTransaction
    private val _startRescue: MutableLiveData<Transaction> by lazy {
        MutableLiveData<Transaction>()
    }
    internal val startRescue: LiveData<Transaction> = _startRescue

    internal fun getShopInfo() {
        FirebaseManager.getAuth()?.currentUser?.uid?.let {
            db.collection(SHOP_COLLECTION)
                .document(it)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val result = documentSnapshot.toObject<Shop>()
                    if (result != null) {
                        if (result.pendingApprove == true) {
                            getShopPendingInfo()
                        } else {
                            if (result.created == true) {
                                _shop.value = result
                                myShop = result
                            } else {
                                _shop.value = null
                                myShop = null
                            }
                            _progressBarStatus.value = false
                        }
                    } else {
                        _shop.value = null
                        myShop = null
                        _progressBarStatus.value = false
                    }
                }
                .addOnFailureListener { exception ->
                    hideProgressbar()
                    Log.e("Main Activity", exception.message.toString())
                    _shop.value = null
                    myShop = null
                }
        }
    }

    private fun getShopPendingInfo() {
        FirebaseManager.getAuth()?.currentUser?.uid?.let {
            db.collection(SHOP_PENDING_COLLECTION)
                .document(it)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val result = documentSnapshot.toObject<Shop>()
                    if (result != null) {
                        _shop.value = result
                        myShop = result
                    } else {
                        _shop.value = null
                        myShop = null
                    }
                    _progressBarStatus.value = false
                }
                .addOnFailureListener { exception ->
                    hideProgressbar()
                    Log.e("Main Activity", exception.message.toString())
                    _shop.value = null
                    myShop = null
                }
        }
    }

    internal fun getShopData() = myShop

    internal fun signOut() {
        removeFcmToken()
        FirebaseManager.getAuth()?.signOut() //End user session
        FirebaseManager.logout()
    }

    private fun removeFcmToken() {
        FirebaseManager.getAuth()?.currentUser?.uid?.let { uid ->
            db.collection(Utils.getCollectionRole()).document(uid).update("fcmToken", "")
        }
    }

    private fun hideProgressbar() {
        _progressBarStatus.value = false
    }

    internal fun navigateToHome() {
        _navigateToHome.value = null
    }

    internal fun getPendingTransaction() {
        db.collection(PENDING_TRANSACTION_COLLECTION)
            .whereArrayContains("shops", FirebaseManager.getAuth()?.uid.toString())
            .get()
            .addOnSuccessListener { documents ->
                pendingTransactions.clear()
                for (document in documents) {
                    val transaction = Transaction().apply {
                        document.data["id"]?.let { id = it as String }
                        document.data["userId"]?.let { userId = it as String }
                        document.data["userFullName"]?.let { userFullName = it as String }
                        document.data["userPhone"]?.let { userPhone = it as String }
                        document.data["service"]?.let { service = it as String }
                        document.data["startTime"]?.let { startTime = it as Double }
                        document.data["content"]?.let { content = it as String }
                        document.data["address"]?.let { address = it as String }
                        document.data["userLocation"]?.let {
                            val location = it as HashMap<*, *>
                            userLocation = LatLng(location["latitude"] as Double, location["longitude"] as Double)
                        }
                        document.data["userFcmToken"]?.let { userFcmToken = it as String }
                    }
                    pendingTransactions.add(transaction)
                }
                _pendingTransaction.value = pendingTransactions
            }
            .addOnFailureListener { exception ->
                Utils.showLongToast(getApplication(), exception.toString())
            }
    }

    internal fun startTransaction(transaction: Transaction, position: Int) {
        transaction.id?.let {
            db.collection(PENDING_TRANSACTION_COLLECTION).document(it).delete()
            val newList = mutableListOf<Transaction>()
            for (index in pendingTransactions.indices) {
                if (index != position) {
                    newList.add(pendingTransactions[index].copy())
                }
            }
            pendingTransactions = newList
            _pendingTransaction.value = pendingTransactions
            FirebaseManager.getAuth()?.uid?.let { id ->
                db.collection(SHOP_COLLECTION).document(id).update("ready", false)
            }
            _startRescue.value = transaction.apply {
                shopId = FirebaseManager.getAuth()?.uid
            }
        }
    }
}
