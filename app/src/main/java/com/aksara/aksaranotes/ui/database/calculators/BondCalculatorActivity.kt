package com.aksara.aksaranotes.ui.database.calculators

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksara.aksaranotes.databinding.ActivityBondCalculatorBinding
import kotlin.math.pow

class BondCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBondCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBondCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCalculator()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Bond Calculator"
    }

    private fun setupCalculator() {
        binding.btnCalculate.setOnClickListener {
            calculateBondPrice()
        }

        binding.btnClear.setOnClickListener {
            clearFields()
        }
    }

    private fun calculateBondPrice() {
        try {
            val faceValue = binding.etFaceValue.text.toString().toDoubleOrNull() ?: 0.0
            val couponRate = binding.etCouponRate.text.toString().toDoubleOrNull() ?: 0.0
            val yearsToMaturity = binding.etYearsToMaturity.text.toString().toDoubleOrNull() ?: 0.0
            val marketRate = binding.etMarketRate.text.toString().toDoubleOrNull() ?: 0.0

            if (faceValue <= 0 || yearsToMaturity <= 0) {
                binding.tvResults.text = "Please enter valid values"
                return
            }

            val couponPayment = faceValue * (couponRate / 100)
            val periods = yearsToMaturity.toInt()
            val discountRate = marketRate / 100

            // Calculate present value of coupon payments
            var pvCoupons = 0.0
            for (i in 1..periods) {
                pvCoupons += couponPayment / (1 + discountRate).pow(i.toDouble())
            }

            // Calculate present value of face value
            val pvFaceValue = faceValue / (1 + discountRate).pow(yearsToMaturity)

            val currentPrice = pvCoupons + pvFaceValue
            val totalReturn = ((faceValue + (couponPayment * periods) - currentPrice) / currentPrice) * 100

            val results = """
                📊 Bond Analysis Results
                
                Face Value: ${"$%.2f".format(faceValue)}
                Coupon Rate: %.2f%%
                Years to Maturity: %.1f years
                Market Rate: %.2f%%
                
                📈 Calculations:
                Annual Coupon Payment: ${"$%.2f".format(couponPayment)}
                Present Value of Coupons: ${"$%.2f".format(pvCoupons)}
                Present Value of Face Value: ${"$%.2f".format(pvFaceValue)}
                
                💰 Results:
                Current Bond Price: ${"$%.2f".format(currentPrice)}
                Total Return: %.2f%%
                
                ${if (currentPrice < faceValue) "💡 Bond trading at discount" else "💡 Bond trading at premium"}
            """.trimIndent().format(couponRate, yearsToMaturity, marketRate, totalReturn)

            binding.tvResults.text = results

        } catch (e: Exception) {
            binding.tvResults.text = "Error in calculation: ${e.message}"
        }
    }

    private fun clearFields() {
        binding.etFaceValue.text?.clear()
        binding.etCouponRate.text?.clear()
        binding.etYearsToMaturity.text?.clear()
        binding.etMarketRate.text?.clear()
        binding.tvResults.text = "Enter bond details above and tap Calculate to see results"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}