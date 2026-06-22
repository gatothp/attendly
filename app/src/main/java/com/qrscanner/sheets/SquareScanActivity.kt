package com.qrscanner.sheets

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * A thin [CaptureActivity] subclass that overrides the scanner layout
 * so the viewfinder framing box is always square (not rectangular).
 *
 * The layout is defined in res/layout/activity_square_scan.xml which
 * references res/layout/zxing_square_finder.xml for the inner finder.
 */
class SquareScanActivity : CaptureActivity()
