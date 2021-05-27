package com.mad.carpooling.ui.maps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.mad.carpooling.R
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint


object MapUtils {
    fun getNumMarker(text: String, ctx: Context): BitmapDrawable {
        val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_map_marker)!!
        val bitmap = drawable.toBitmap()

        val paint = Paint();
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16F, ctx.resources.displayMetrics
        )

        val canvas = Canvas(bitmap)
// Note, this code places the text in the center of the Bitmap (both vertically and horizontally)
// https://stackoverflow.com/a/11121873
        canvas.drawText(
            text, bitmap.width / 2f,
            bitmap.height / 2.5f - (paint.descent() + paint.ascent() / 2), paint
        )

        return BitmapDrawable(ctx.resources, bitmap)
    }

    fun computeArea(gp: ArrayList<GeoPoint>): BoundingBox? {
        var north = -90.0
        var south = 90.0
        var west = 180.0
        var east = -180.0
        for (position in gp) {
            north = position.latitude.coerceAtLeast(north)
            south = position.latitude.coerceAtMost(south)
            west = position.longitude.coerceAtMost(west)
            east = position.longitude.coerceAtLeast(east)
        }
        return BoundingBox(north, east, south, west)
    }
}