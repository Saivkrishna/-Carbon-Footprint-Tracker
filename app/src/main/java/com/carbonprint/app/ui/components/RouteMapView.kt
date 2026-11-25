package com.carbonprint.app.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.carbonprint.app.domain.location.ProcessedRoute
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Interactive OpenStreetMap route visualizer for Jetpack Compose using osmdroid.
 * Follows OpenStreetMap tile usage policies by specifying custom user agent, local caching,
 * and standard attribution overlay.
 */
@Composable
fun RouteMapView(
    processedRoute: ProcessedRoute,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize osmdroid user-agent according to OSM Tile Usage Policy
    remember {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    fun centerAndZoomRoute(map: MapView) {
        val points = processedRoute.points
        if (points.isEmpty()) return
        val bbox = processedRoute.boundingBox
        if (bbox != null && points.size >= 2) {
            val osmBox = BoundingBox(
                bbox.maxLatitude,
                bbox.maxLongitude,
                bbox.minLatitude,
                bbox.minLongitude
            )
            map.post {
                try {
                    map.zoomToBoundingBox(osmBox.increaseByScale(1.4f), true)
                } catch (e: Exception) {
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(GeoPoint(points.first().latitude, points.first().longitude))
                }
            }
        } else {
            val first = points.first()
            map.controller.setZoom(16.0)
            map.controller.setCenter(GeoPoint(first.latitude, first.longitude))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0F2F1))
    ) {
        AndroidView(
            factory = { mapView },
            update = { map ->
                map.overlays.clear()

                // 1. OpenStreetMap Copyright attribution overlay
                val copyrightOverlay = CopyrightOverlay(context)
                copyrightOverlay.setTextSize(9)
                map.overlays.add(copyrightOverlay)

                val points = processedRoute.points
                if (points.isNotEmpty()) {
                    val geoPoints = ArrayList<GeoPoint>(points.size)
                    for (p in points) {
                        geoPoints.add(GeoPoint(p.latitude, p.longitude, p.altitude))
                    }

                    // 2. Journey Polyline connecting GPS points in chronological order
                    val polyline = Polyline(map).apply {
                        setPoints(geoPoints)
                        outlinePaint.apply {
                            color = AndroidColor.parseColor("#00796B") // EmeraldDark
                            strokeWidth = 9f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        title = "Travel Route (${points.size} GPS points)"
                    }
                    map.overlays.add(polyline)

                    // 3. Green Start Marker
                    val start = points.first()
                    val startMarker = Marker(map).apply {
                        position = GeoPoint(start.latitude, start.longitude)
                        title = "Start Point"
                        snippet = "Lat: ${String.format("%.4f", start.latitude)}, Lon: ${String.format("%.4f", start.longitude)}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon?.setTint(AndroidColor.parseColor("#2E7D32")) // Green
                    }
                    map.overlays.add(startMarker)

                    // 4. Red Destination Marker
                    if (points.size >= 2) {
                        val dest = points.last()
                        val destMarker = Marker(map).apply {
                            position = GeoPoint(dest.latitude, dest.longitude)
                            title = "Destination"
                            snippet = "Lat: ${String.format("%.4f", dest.latitude)}, Lon: ${String.format("%.4f", dest.longitude)}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon?.setTint(AndroidColor.parseColor("#D32F2F")) // Red
                        }
                        map.overlays.add(destMarker)
                    }

                    // 5. Initial zoom and center to route
                    centerAndZoomRoute(map)
                }
                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Map Control Floating Buttons (Zoom In, Zoom Out, Re-Center)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.92f), CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { mapView.controller.zoomIn() },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = EmeraldDark)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
                }
                IconButton(
                    onClick = { mapView.controller.zoomOut() },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = EmeraldDark)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
                }
                IconButton(
                    onClick = { centerAndZoomRoute(mapView) },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = "Fit Route")
                }
            }
        }

        // OSM Legal Attribution Tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                fontSize = 9.sp,
                color = Color.DarkGray
            )
        }
    }
}
