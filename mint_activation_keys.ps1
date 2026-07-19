# Mint full activation keys for distribution to paying customers.
# Keys are HMAC-SHA256 signed with your secret, matching ActivationKeyGenerator.kt.
# Keep this file (and the secret) private. Never ship it inside the app.
param(
    [int]$Count = 1,
    [string]$Secret = $env:LICENSE_SECRET
)

if ([string]::IsNullOrWhiteSpace($Secret)) {
    Write-Error "Set the secret: -Secret '...' or `$env:LICENSE_SECRET. Refusing to mint with empty secret."
    exit 1
}

$Alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
$Prefix = "STN"
$BodyLen = 17
$SignLen = 4
$Total = $Prefix.Length + $BodyLen + $SignLen
$Base = $Alphabet.Length

function Get-Sign([string]$core) {
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [Text.Encoding]::UTF8.GetBytes($Secret)
    $raw = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($core))
    $value = 0L
    for ($i = 0; $i -lt $SignLen; $i++) { $value = ($value * 256 + ($raw[$i] -band 0xFF)) -band 0xFFFFFFFFL }
    $mod = $value % [long][math]::Pow($Base, $SignLen)
    $sb = New-Object System.Text.StringBuilder
    $m = $mod
    for ($i = 0; $i -lt $SignLen; $i++) { $sb.Insert(0, $Alphabet[$m % $Base]) | Out-Null; $m = [long]($m / $Base) }
    return $sb.ToString()
}

function New-Key {
    $body = ""
    for ($i = 0; $i -lt $BodyLen; $i++) { $body += $Alphabet[(Get-Random -Minimum 0 -Maximum $Base)] }
    $core = $Prefix + $body
    return $core + (Get-Sign $core)
}

$keys = @(); $seen = @{}
while ($keys.Count -lt $Count) {
    $k = New-Key
    if (-not $seen.ContainsKey($k)) { $seen[$k] = $true; $keys += $k }
}
$keys | ForEach-Object { $_ }
