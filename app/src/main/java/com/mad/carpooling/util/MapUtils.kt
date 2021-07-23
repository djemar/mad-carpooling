package com.mad.carpooling.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.mad.carpooling.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.MapSnapshotable
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.util.concurrent.atomic.AtomicInteger


object MapUtils {
    fun getNumMarker(text: String, ctx: Context): BitmapDrawable {
        val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_map_marker)!!
        val bitmap = drawable.toBitmap()

        val paint = Paint()
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

    fun redrawMarkers(waypoints: ArrayList<Marker>, mapView: MapView, ctx: Context) {
        val count: AtomicInteger = AtomicInteger(0)
        waypoints.forEach { marker ->
            marker.icon =
                getNumMarker((count.incrementAndGet()).toString(), ctx)
        }
        mapView.invalidate()
    }

    fun mapToBitmap(
        overlays: MutableList<Overlay>,
        box: BoundingBox?,
        zoom: Double,
        mapTileProvider: MapTileProviderBasic,
        mapSize: Int,
        ivMap: ImageView
    ) {
        val mapSnapshot = MapSnapshot(
            MapSnapshotable { pMapSnapshot ->
                if (pMapSnapshot.status != MapSnapshot.Status.CANVAS_OK) {
                    return@MapSnapshotable
                }
                val bitmap = Bitmap.createBitmap(pMapSnapshot.bitmap)
                ivMap.setImageBitmap(bitmap)

            }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProvider, overlays,
            Projection(
                zoom, mapSize, mapSize, box?.centerWithDateLine,
                0F,
                true,
                true,
                0,
                0
            )
        )
        Thread(mapSnapshot).start()
    }
}