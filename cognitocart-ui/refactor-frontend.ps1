$files = Get-ChildItem -Path "src\app" -Recurse -Include *.ts, *.html

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw
    $originalContent = $content
    
    if ($content -match "imageUrls") {
        # Simple property replacements
        $content = $content -replace "imageUrls\?\.\[0\]", "mediaGallery?.[0]?.mediaUrl"
        $content = $content -replace "imageUrls\[0\]", "mediaGallery[0]?.mediaUrl"
        $content = $content -replace "imageUrls\?\.\[1\]", "mediaGallery?.[1]?.mediaUrl"
        $content = $content -replace "imageUrls\[1\]", "mediaGallery[1]?.mediaUrl"
        $content = $content -replace "imageUrls\?\.length", "mediaGallery?.length"
        $content = $content -replace "imageUrls\.length", "mediaGallery?.length"
        
        # product-detail.component.ts specific replacements
        $content = $content -replace "let img of product\.imageUrls", "let media of product.mediaGallery"
        $content = $content -replace "let img of r\.imageUrls", "let media of r.mediaGallery"
        $content = $content -replace "\[src\]=""img""", "[src]=""media.mediaUrl"""
        $content = $content -replace "\[src\]=""img\?\.mediaUrl""", "[src]=""media.mediaUrl"""
        
        # admin-reviews.component.ts & admin-returns.component.ts
        $content = $content -replace "let img of review\.imageUrls", "let media of review.mediaGallery"
        $content = $content -replace "let img of r\.imageUrls", "let media of r.mediaGallery"
        
        # product-media-tab.component.ts
        $content = $content -replace "let img of product\.imageUrls", "let media of product.mediaGallery"
        $content = $content -replace "if \(\!this\.product\.imageUrls\)", "if (!this.product.mediaGallery)"
        $content = $content -replace "this\.product\.imageUrls = \[\];", "this.product.mediaGallery = [];"
        $content = $content -replace "this\.product\.imageUrls\.push\((.*\.imageUrl)\);", "this.product.mediaGallery.push({ mediaUrl: `$1, isPrimary: false, sortOrder: 99 });"
        $content = $content -replace "this\.product\.imageUrls = this\.product\.imageUrls\.filter\(\(url: string\) => url !== imageUrl\);", "this.product.mediaGallery = this.product.mediaGallery.filter((m: any) => m.mediaUrl !== imageUrl);"
        
        # other catch-alls (if any remaining)
        $content = $content -replace "\.imageUrls", ".mediaGallery"
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8
            Write-Host "Updated $($file.Name)"
        }
    }
}
