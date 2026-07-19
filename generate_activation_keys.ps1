# Activation Key Generator (standalone Windows runner)
# Scheme matches ActivationKeyGenerator.kt / LicenseManager:
#   24 chars, uppercase alphanumeric, "STN" prefix, last 4 chars = base36 checksum of first 20.
param(
    [int]$Count = 1
)

$Alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
$Prefix = "STN"
$BodyLen = 17
$CheckLen = 4
$Total = $Prefix.Length + $BodyLen + $CheckLen
$Base = $Alphabet.Length

function Get-Checksum([string]$core) {
    $sum = 0
    foreach ($c in $core.ToCharArray()) { $sum += [int][char]$c }
    $mod = [long]($sum % [long][math]::Pow($Base, $CheckLen))
    $sb = New-Object System.Text.StringBuilder
    for ($i = 0; $i -lt $CheckLen; $i++) {
        $sb.Insert(0, $Alphabet[$mod % $Base]) | Out-Null
        $mod = [long]($mod / $Base)
    }
    return $sb.ToString()
}

function New-ActivationKey {
    $body = ""
    for ($i = 0; $i -lt $BodyLen; $i++) {
        $body += $Alphabet[(Get-Random -Minimum 0 -Maximum $Base)]
    }
    $core = $Prefix + $body
    return $core + (Get-Checksum $core)
}

$keys = @()
$seen = @{}
while ($keys.Count -lt $Count) {
    $k = New-ActivationKey
    if (-not $seen.ContainsKey($k)) {
        $seen[$k] = $true
        $keys += $k
    }
}

$keys | ForEach-Object { $_ }
