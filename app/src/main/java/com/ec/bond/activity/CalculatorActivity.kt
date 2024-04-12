package com.ec.bond.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ec.bond.R
import com.ec.bond.helper.*
import com.ec.bond.utils.Constant
import com.ec.bond.utils.SharePreferenceUtility
import kotlinx.android.synthetic.main.activity_calculator.*

class CalculatorActivity : AppCompatActivity(), Calculator, View.OnClickListener {
    lateinit var calc: CalculatorImpl
    private var vibrateOnButtonPress = true
    var autoLogin:Boolean=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        autoLogin = SharePreferenceUtility.getPreferences(this, Constant.IS_AUTO_LOGIN, SharePreferenceUtility.PREFTYPE_BOOLEAN) as Boolean
        val isLogin: Boolean = SharePreferenceUtility.getPreferences(this, Constant.IS_REGISTER_DONE, SharePreferenceUtility.PREFTYPE_BOOLEAN) as Boolean

        if (isLogin){
            if (autoLogin){
                startActivity(Intent(this@CalculatorActivity, SplashScreenActivity::class.java))
                finish()
            }else{

            }

        }else{
            startActivity(Intent(this@CalculatorActivity, SplashScreenActivity::class.java))
            finish()
        }


        calc = CalculatorImpl(this, applicationContext)
        initListner()
    }

    private fun initListner() {
       var l1:LinearLayout= findViewById<LinearLayout>(R.id.oneLy)
        var l2= findViewById<LinearLayout>(R.id.twoLy)
        var l3=findViewById<LinearLayout>(R.id.threeLy)
        var l5= findViewById<LinearLayout>(R.id.fourLy)
        var l6=findViewById<LinearLayout>(R.id.fiveLy)
        var l7=findViewById<LinearLayout>(R.id.sixLy)
        var l8=findViewById<LinearLayout>(R.id.sevenLy)
        var l9=findViewById<LinearLayout>(R.id.eightLy)
        var l10=findViewById<LinearLayout>(R.id.nineLy)
        var l11=findViewById<LinearLayout>(R.id.DoublezeroLy)
        var l12=findViewById<LinearLayout>(R.id.dotLy)

        var l13=findViewById<LinearLayout>(R.id.equalLy).setOnClickListener(this)
        var l14=findViewById<LinearLayout>(R.id.plusLy).setOnClickListener(this)
        var l15=findViewById<LinearLayout>(R.id.minusLy).setOnClickListener(this)
        var l16=findViewById<LinearLayout>(R.id.multiplyLy).setOnClickListener(this)
        var l17=findViewById<LinearLayout>(R.id.zeroLy)

        var l18=findViewById<LinearLayout>(R.id.clearLy).setOnClickListener(this)
        var l19=findViewById<LinearLayout>(R.id.percentageLy).setOnClickListener(this)
        var l20= findViewById<LinearLayout>(R.id.eraseLy).setOnClickListener(this)
        var l21=findViewById<LinearLayout>(R.id.divideLy).setOnClickListener(this)

      var array= arrayOf(l1,l2,l3,l5,l6,l7,l8,l9,l10,l11,l12,l17)
          array .forEach {
              it.setOnClickListener { calc.numpadClicked(it.id); checkHaptic(it) }
          }

    }

    override fun showNewResult(value: String, context: Context) {
        findViewById<TextView>(R.id.resultTv).setText(value)
    }

    override fun showNewFormula(value: String, context: Context) {

    }

    override fun onClick(p0: View?) {

        when(p0?.id){
            R.id.equalLy -> {
                var result=resultTv.text.toString()
                result= result.replace("×".toRegex(),"*").replace(",".toRegex(),"")
                Log.e("result",result+"=="+SharePreferenceUtility.getPreferences(this@CalculatorActivity, Constant.IS_ACTIVATION_CODE, SharePreferenceUtility.PREFTYPE_STRING))


                if(result.equals(SharePreferenceUtility.getPreferences(this@CalculatorActivity, Constant.IS_ACTIVATION_CODE, SharePreferenceUtility.PREFTYPE_STRING))){
                    startActivity(Intent(this@CalculatorActivity, SplashScreenActivity::class.java))
                    finish()
                }else{
                    calc.handleEquals(); checkHaptic(p0)
                }

            }
            R.id.plusLy -> {
                calc.handleOperation(PLUS); checkHaptic(p0)
            }
            R.id.minusLy -> {
                calc.handleOperation(MINUS); checkHaptic(p0)
            }
            R.id.multiplyLy -> {
                calc.handleOperation(MULTIPLY); checkHaptic(p0)
            }
            R.id.clearLy -> {
                calc.handleReset(); checkHaptic(p0)
            }
            R.id.percentageLy -> {
                calc.handleOperation(PERCENT); checkHaptic(p0)
            }
            R.id.eraseLy -> {
                calc.handleClear(); checkHaptic(p0)
            }

            R.id.divideLy -> {
                calc.handleOperation(DIVIDE); checkHaptic(p0)
            }


        }

    }

    private fun checkHaptic(view: View) {
        if (vibrateOnButtonPress) {
            //view.performHapticFeedback()
        }
    }
}