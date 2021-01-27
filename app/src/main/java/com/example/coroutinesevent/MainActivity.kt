package com.example.coroutinesevent

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {

    private val store: Store by viewModels()
    private val actionCreator: ActionCreator by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate")

        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.button)!!.setOnClickListener {
            actionCreator.execute()
        }

        // LiveData
        val textView1 = findViewById<TextView>(R.id.text1)!!
        textView1.text = "LiveData\n"
        store.liveData.observe(this, Observer { count ->
            // 最新のイベントのみ受け取られる
            // rotation後も最新のイベントが受け取れる
            log("LiveData : $count")
            textView1.log(count)
        })

        // StateFlow
        val textView2 = findViewById<TextView>(R.id.text2)!!
        textView2.text = "StateFlow\n"
        lifecycleScope.launchWhenStarted {
            // 最新のイベントのみ受け取られる
            // rotation後も最新のイベントが受け取れる
            store.stateFlow.collect { count ->
                log("StateFlow : $count")
                textView2.log(count)
            }
        }

        // Shred StateFlow
        val textView3 = findViewById<TextView>(R.id.text3)!!
        textView3.text = "SharedStateFlow\n"
        lifecycleScope.launchWhenStarted {
            // replay=0 -> collect後に来たイベントのみ受け取られる
            // replay>0 -> 最新の値が受け取れる (もとはStateFlowなので? replayを2以上にしても意味がない)
            store.stateFlow.shareIn(lifecycleScope, SharingStarted.Eagerly, replay = 0).collect { count ->
                log("StateFlow shareIn: $count")
                textView3.log(count)
            }
        }

        // SharedFlow
        val textView4 = findViewById<TextView>(R.id.text4)!!
        textView4.text = "SharedFlow\n"
        lifecycleScope.launchWhenStarted {
            // replay=0 -> collect後に来たイベントのみ受け取られる
            // replay>0 -> replayの数だけ最新の値が受け取れる
            store.sharedFlow.collect { count ->
                log("launch SharedFlow : $count")
                textView4.log(count)
            }
        }

        // こういう書き方もできる。
        // ネストが減るが、単なるlaunchInを使ってしまう可能性もある
        // https://developer.android.com/kotlin/flow/stateflow-and-sharedflow#stateflow
        // > Never collect a flow from the UI using launch or the launchIn extension function if the UI needs to be updated.
        store.stateFlow.onEach {
//            log("StateFlow : $count")
//            textView2.log(count)
        }.launchWhenStartedIn(this)
    }

    override fun onStart() {
        super.onStart()
        log("onStart")
    }

    override fun onResume() {
        super.onResume()
        log("onResume")
    }

    override fun onPause() {
        super.onPause()
        log("onPause")
    }

    override fun onStop() {
        super.onStop()
        log("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
    }

    @SuppressLint("SetTextI18n")
    private fun TextView.log(count: Int) {
        text = text.toString() + "${count}\n"
    }

    // ref: https://qiita.com/takahirom/items/e9294bfc4f3b1bbee696#flow%E3%82%92started%E4%BB%A5%E9%99%8D%E3%81%AE%E7%8A%B6%E6%85%8B%E3%81%A7%E5%8B%95%E3%81%8B%E3%81%99%E3%81%AB%E3%81%AF
    private fun <T> Flow<T>.launchWhenStartedIn(
        lifecycleOwner: LifecycleOwner
    ) = lifecycleOwner.lifecycleScope.launchWhenStarted {
        collect()
    }

    private fun log(text: String) {
        Log.d("__", text)
    }
}

class Store : ViewModel(), Dispatcher.Listener {
    private val _liveData = MutableLiveData(-99)
    val liveData: LiveData<Int> = _liveData

    private val _stateFlow = MutableStateFlow(-99)
    val stateFlow: StateFlow<Int> = _stateFlow

    // replayの数だけ再度流してくれる
    private val _sharedFlow = MutableSharedFlow<Int>(replay = 0)
    val sharedFlow: SharedFlow<Int> = _sharedFlow

    init {
        Dispatcher.register(this)
    }

    override fun onCleared() {
        Dispatcher.unregister(this)
        super.onCleared()
    }

    override fun on(action: Any) {
        when (action) {
            is CounterAction -> {
                val count = action.count

                _liveData.value = count
                _stateFlow.value = count
                viewModelScope.launch {
                    _sharedFlow.emit(count)
                }
            }
            else -> {
            }
        }
    }
}


class ActionCreator : ViewModel() {
    private val counter = AtomicInteger()

    fun execute() = viewModelScope.launch {
        delay(1000)
        Dispatcher.dispatch(CounterAction(counter.getAndIncrement()))
    }
}

data class CounterAction(val count: Int)
