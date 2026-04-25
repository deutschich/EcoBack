# BalSync 1.0.1

## New:

- **New Feature: Configurable Update Check Interval**  
>You can now control how often EcoBack checks for new versions.
>
>- Added `update-check-interval` and `update-check-unit` options to `config.yml`  
  (e.g., interval `12` and unit `"hours"` = check every 12 hours)
>- The update checker runs automatically on startup and then repeats at the configured interval
>- Set `update-check-interval` to `0` to disable periodic checks completely
>- Works alongside the existing join notification for players with the `ecoback.update.notify` permission
>
> **Important:** Please update your config.yml to use the new feature correctly!
> 
>📝 **Example configuration:**
>```yaml
>update-check-interval: 30
>update-check-unit: "minutes"
>```
>
>No other changes – fully backward compatible.