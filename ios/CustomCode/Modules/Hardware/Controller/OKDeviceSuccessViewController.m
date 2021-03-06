//
//  OKDeviceSuccessViewController.m
//  OneKey
//
//  Created by xiaoliang on 2020/12/11.
//  Copyright © 2020 OneKey. All rights reserved.
//

#import "OKDeviceSuccessViewController.h"

@interface OKDeviceSuccessViewController ()
@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet UIImageView *iconImageView;
@property (weak, nonatomic) IBOutlet UIView *nameBgView;
@property (weak, nonatomic) IBOutlet UILabel *nameLabel;
@property (weak, nonatomic) IBOutlet UILabel *descLabel;
@property (weak, nonatomic) IBOutlet UIButton *completeBtn;
- (IBAction)completeBtnClick:(UIButton *)sender;
@property (nonatomic,assign)OKDeviceSuccessType type;
@property (nonatomic,copy)NSString *deviceName;
@end

@implementation OKDeviceSuccessViewController

+ (instancetype)deviceSuccessViewController:(OKDeviceSuccessType)type deviceName:(NSString *)deviceName;
{
    OKDeviceSuccessViewController *vc = [[UIStoryboard storyboardWithName:@"Hardware" bundle:nil]instantiateViewControllerWithIdentifier:@"OKDeviceSuccessViewController"];
    vc.type = type;
    vc.deviceName = deviceName;
    return vc;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [self stupUI];
}

- (void)stupUI
{
    switch (_type) {
        case OKDeviceSuccessActivate:
        {
            self.title = MyLocalizedString(@"Activate hardware wallet", nil);
            self.title = MyLocalizedString(@"Wallet activation successful", nil);
            [self.descLabel setText:MyLocalizedString(@"Your hardware wallet has been successfully activated and we have nothing to remind you of. In a word, please take good care of it. No one can help you get it back. I wish you play in the chain of blocks in the world happy", nil) lineSpacing:20];
        }
            break;
        case OKDeviceSuccessHwBackup:
        {
            self.title = MyLocalizedString(@"Backup the purse", nil);
            self.titleLabel.text = MyLocalizedString(@"You're done", nil);
            [self.descLabel setText:MyLocalizedString(@"Your mnemonic has been successfully backed up to this device, and we have nothing more to remind you of. In a word, remember to take good care of it, lost no one can help you find it. Have fun in the world of blockchain.", nil) lineSpacing:20];
        }
            break;
        default:
            break;
    }
    self.iconImageView.image = [UIImage imageNamed:@"device_success"];
    self.nameLabel.text = self.deviceName;
    [self.completeBtn setLayerDefaultRadius];
    [self.nameBgView setLayerDefaultRadius];
}
- (IBAction)completeBtnClick:(UIButton *)sender {
    [self.OK_TopViewController dismissToViewControllerWithClassName:@"OKWalletViewController" animated:YES complete:^{
        
    }];
}
@end
