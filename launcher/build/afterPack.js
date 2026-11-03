const fs = require('fs');
const path = require('path');
const ResEdit = require('resedit');

exports.default = async function afterPack(context) {
  if (context.electronPlatformName !== 'win32') return;
  const { productFilename, version } = context.packager.appInfo;
  const exe = path.join(context.appOutDir, `${productFilename}.exe`);
  const ico = path.join(__dirname, 'icon.ico');

  const exeObj = ResEdit.NtExecutable.from(fs.readFileSync(exe), { ignoreCert: true });
  const res = ResEdit.NtExecutableResource.from(exeObj);

  const iconFile = ResEdit.Data.IconFile.from(fs.readFileSync(ico));
  const groups = ResEdit.Resource.IconGroupEntry.fromEntries(res.entries);
  const target = groups[0] || { id: 1, lang: 1033 };
  ResEdit.Resource.IconGroupEntry.replaceIconsForResource(res.entries, target.id, target.lang, iconFile.icons.map(i => i.data));

  const [major, minor, patch] = version.split('.').map(n => parseInt(n, 10) || 0);
  const vi = ResEdit.Resource.VersionInfo.fromEntries(res.entries)[0] || ResEdit.Resource.VersionInfo.createEmpty();
  vi.setFileVersion(major, minor, patch, 0, 1033);
  vi.setProductVersion(major, minor, patch, 0, 1033);
  vi.setStringValues({ lang: 1033, codepage: 1200 }, {
    ProductName: 'Surge Client',
    FileDescription: 'Surge Client',
    CompanyName: 'Surge',
    OriginalFilename: `${productFilename}.exe`,
    InternalName: 'Surge Client',
    FileVersion: version,
    ProductVersion: version,
    LegalCopyright: '',
  });
  vi.outputToResourceEntries(res.entries);

  res.outputResource(exeObj);
  fs.writeFileSync(exe, Buffer.from(exeObj.generate()));
};
