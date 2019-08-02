package com.wz.five.banner

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        cb_test.setAutoScroll(false)
        cb_test.setDefaultImage(R.drawable.ic_launcher_background)
        cb_test.setScrollDelayTime(3000)
        cb_test.setScrollSpeed(0.5f)

        val dataList = ArrayList<Int>()
        dataList.add(R.drawable.img_deault_1)
        dataList.add(R.drawable.img_deault_2)
        dataList.add(R.drawable.img_deault_3)
        dataList.add(R.drawable.img_deault_4)

        cb_test.setImageList(dataList)

        val list = arrayListOf(
                "https://desk-fd.zol-img.com.cn/t_s960x600c5/g2/M00/05/05/ChMlWV1AKjqIT0VJAAROxI8NfYgAAMOugMIBxQABE7c826.jpg",
                "https://desk-fd.zol-img.com.cn/t_s960x600c5/g2/M00/05/05/ChMlWV1AHyqIENlrAAdQkZuBF1IAAMOmgDKBi4AB1Cp466.jpg",
                "https://desk-fd.zol-img.com.cn/t_s960x600c5/g2/M00/05/00/ChMlWV0_q62IVR-QAAtf-zjcv88AAMNbwJvv18AC2AT314.jpg",
                "https://desk-fd.zol-img.com.cn/t_s960x600c5/g2/M00/05/00/ChMlWl0_qG-IEABzAARkaBCdGI4AAMNZAEMV6IABGSA880.jpg"
        )
        btn_default.setOnClickListener {
            cb_test.updateImageList(emptyList())
        }
        btn_update.setOnClickListener {
            cb_test.updateImageList(list)
        }
        btn_reset.setOnClickListener {
            cb_test.updateImageList(dataList)
        }
    }

    override fun onResume() {
        super.onResume()
        cb_test.startAutoScroll()
    }

    override fun onPause() {
        cb_test.stopAutoScroll()
        super.onPause()
    }
}
